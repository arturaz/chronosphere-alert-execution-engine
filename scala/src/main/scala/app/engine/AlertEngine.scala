package app.engine

import app.data.MaxConcurrentRequests
import app.data.httpapi.alerts._
import app.utils.IOUtils
import cats.Parallel
import cats.effect.std.{Hotswap, Semaphore}
import cats.effect.{IO, Ref, Resource, ResourceIO}
import cats.implicits.toTraverseOps
import com.emarsys.logger.{Logging, LoggingContext}

import scala.concurrent.duration.DurationInt

object AlertEngine {
  /** Gives you a permit to do a HTTP request. */
  type AcquirePermit = ResourceIO[Unit]

  trait FetchQueries {
    def apply(logCtx: LoggingContext): IO[QueryAlertsResponse]
  }
  trait FetchQueryValue {
    def apply(acquirePermit: AcquirePermit, queryName: QueryName): LoggingContext => IO[QueryValue]
  }
  trait ReportNotify {
    def apply(acquirePermit: AcquirePermit, request: NotifyRequest): LoggingContext => IO[Unit]
  }
  trait ReportResolve {
    def apply(acquirePermit: AcquirePermit, request: ResolveRequest): LoggingContext => IO[Unit]
  }

  /**
   * Launches the [[AlertEngine]]. The [[IO]] never returns, unless either of these conditions occur:
   *
   * 1) It's cancelled.
   *
   * 2) The value of [[QueryAlertsResponse.alerts]] is empty, then it returns immediately after fetch, as it does not
   * have any [[Alert]]s to monitor.
   **/
  def initialize(
    fetchQueries: FetchQueries,
    fetchQueryValue: FetchQueryValue,
    reportNotify: ReportNotify,
    reportResolve: ReportResolve,
    maxConcurrentAlertExecutions: MaxConcurrentRequests
  )(implicit log: Logging[IO]): IO[Unit] = {
    implicit val logCtx = LoggingContext("main")

    for {
      _ <- log.info("Fetching queries...")
      alerts <- fetchQueries.apply(logCtx)
      _ <- log.info(s"Queries fetched:\n${alerts.alerts.mkString("\n")}")
      semaphore <- Semaphore[IO](maxConcurrentAlertExecutions)
      alertStates <- alerts.alerts.map { alert =>
        Ref.of[IO, AlertState](AlertState.Pass).map { alertStateRef =>
          alert -> alertStateRef
        }
      }.sequence.map(_.toMap)
      alertStatesStrIO = alertStates.iterator.map { case (alert, ref) =>
        ref.get.map(alertState => s"${alert.name}: $alertState")
      }.toVector.sequence.map { stateStrings =>
        s"Alert states: [\n${stateStrings.sorted.iterator.map(_.indent(2)).mkString("")}]"
      }
      logAlertStatesIO = alertStatesStrIO.flatMap(str => log.info(str))
      handlers = alertStates.iterator.map { case (alert, alertStateRef) =>
        def create(implicit logCtx: LoggingContext) = {
          def acquirePermitIO(name: String) =
            Resource.make {
              log.debug(s"Acquiring permit: $name")
            } { _ =>
              log.debug(s"Releasing permit: $name")
            }.flatMap { _ =>
              semaphore.permit.evalMap(_ => log.debug(s"Acquired permit: $name"))
            }

          alertHandler(alert, acquirePermitIO, alertStateRef, fetchQueryValue, reportNotify, reportResolve)
        }

        create(logCtx.copy(transactionId = alert.name))
      }.toVector
      // Log alert states every 10 seconds
      _ <- (logAlertStatesIO >> IO.sleep(10.seconds)).foreverM.start.bracket(
        // Sequence all IOs, which will never return, as alert handlers never return once created.
        use = _ => Parallel.parSequence_(handlers)
      )(
        // Stop reporting alert states when the handlers finish.
        release = _.cancel
      )

    } yield ()
  }

  /** Create a handler for the [[Alert]] which never returns. */
  def alertHandler(
    alert: Alert, createAcquirePermit: String => AcquirePermit, alertStateRef: Ref[IO, AlertState],
    fetchQueryValue: FetchQueryValue,
    reportNotify: ReportNotify,
    reportResolve: ReportResolve,
  )(implicit log: Logging[IO], logCtx: LoggingContext): IO[Nothing] = {
    /**
     * Takes the current [[AlertState]] from the [[Ref]], invokes `singleExecutionRun` and stores the resulting
     * state back to the [[Ref]]. */
    def singleExecutionRunRef(alertStateRef: Ref[IO, AlertState]) =
      for {
        currentState <- alertStateRef.get
        result <- singleExecutionRun(currentState)
        _ <- alertStateRef.set(result.newState)
      } yield result.reporterResource

    case class SingleExecutionRunResult(newState: AlertState, reporterResource: ResourceIO[Unit])

    /**
     * Queries the alert value, determines the new state and returns a resource that can be used to start
     * reporting of the new alert state.
     * */
    def singleExecutionRun(currentState: AlertState) = for {
      _ <- log.info(s"Fetching ${alert.query}, current state: $currentState")
      queryValue <- fetchQueryValue(createAcquirePermit(s"fetchQueryValue(${alert.query})"), alert.query)(logCtx)
      _ <- log.info(s"Fetched ${alert.query}: $queryValue")
      newState = alert.thresholds.stateFor(queryValue.asAlertThreshold)
      (logIO, resource) =
        if (currentState == newState) {
          // Returns a resource that will do nothing
          val logIO = log.debug("State kept the same")
          (logIO, Resource.unit[IO])
        } else {
          val logIO = log.info(s"State change: $currentState -> $newState")
          val reporterIO = newState match {
            case AlertState.Pass =>
              // Report once
              val request = ResolveRequest(alert.name)
              reportResolve(createAcquirePermit(s"reportResolve($request)"), request)(logCtx)
            case state: AlertState.NotifyState =>
              val request = NotifyRequest(alert.name, state.message)
              // Report forever every repeat interval until cancelled.
              IOUtils.timedCountingRepeater(
                repeatEvery = alert.repeatInterval,
                action = index => {
                  val name = s"reportNotify(index=$index, $request)"
                  reportNotify(createAcquirePermit(name), request)(logCtx) *>
                    log.info(s"$name done, will run again after ${alert.repeatInterval}")
                }
              )
          }
          // Returns a resource which will start the reporting in the background.
          val resource = reporterIO.background.map(_ => () /* ignore the IO that allows us to know how the fiber ended */)
          (logIO, resource)
        }
      _ <- logIO
    } yield SingleExecutionRunResult(newState, resource)

    /** Repeats a single execution forever. */
    def repeatedExecution(
      reporterHotswap: Hotswap[IO, Unit]
    ) = {
      val io = singleExecutionRunRef(alertStateRef).flatMap(reporterHotswap.swap) *>
        log.info(s"Going to sleep for ${alert.interval}") *>
        IO.sleep(alert.interval)
      io.foreverM
    }

    val handler =
      log.info(s"Starting handler for:\n${alert.debugString}") *>
        // Use the hotswap to ensure previous reporter is stopped when the new one is launched.
        Hotswap.create[IO, Unit].use(repeatedExecution)
    handler
  }
}
