package app.engine

import app.data.MaxConcurrentAlertExecutions
import app.data.httpapi.alerts._
import app.utils.IOUtils
import cats.Parallel
import cats.effect.std.{Hotswap, Semaphore}
import cats.effect.{Clock, IO, Ref, Resource, ResourceIO}
import cats.implicits.toTraverseOps
import com.emarsys.logger.{Logging, LoggingContext}

import scala.concurrent.duration.DurationInt

object AlertEngine {
  /**
   * Launches the [[AlertEngine]]. The [[IO]] never returns, unless either of these conditions occur:
   *
   * 1) It's cancelled.
   *
   * 2) The value of [[QueryAlertsResponse.alerts]] is empty, then it returns immediately after fetch, as it does not
   * have any [[Alert]]s to monitor.
   **/
  def initialize(
    fetchQueries: IO[QueryAlertsResponse],
    fetchQueryValue: QueryName => IO[QueryValue],
    reportNotify: NotifyRequest => IO[Unit],
    reportResolve: ResolveRequest => IO[Unit],
    maxConcurrentAlertExecutions: MaxConcurrentAlertExecutions
  )(implicit log: Logging[IO]): IO[Unit] = {
    implicit val logCtx = LoggingContext("main")

    for {
      _ <- log.info("Fetching queries...")
      alerts <- fetchQueries
      _ <- log.info("Queries fetched")
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
        alertHandler(alert, semaphore, alertStateRef, fetchQueryValue, reportNotify, reportResolve)(
          log, logCtx.addParameter("alert" -> alert.name.name)
        )
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
    alert: Alert, concurrentExecutions: Semaphore[IO], alertStateRef: Ref[IO, AlertState],
    fetchQueryValue: QueryName => IO[QueryValue],
    reportNotify: NotifyRequest => IO[Unit],
    reportResolve: ResolveRequest => IO[Unit],
  )(implicit log: Logging[IO], loggingContext: LoggingContext): IO[Nothing] = {
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
      queryValue <- fetchQueryValue(alert.query)
      _ <- log.info(s"Fetched ${alert.query}: $queryValue")
      newState = alert.thresholds.stateFor(queryValue.asAlertThreshold)
      _ <- log.info(s"New state = $newState")
      resource =
        if (currentState == newState) {
          // Returns a resource that will do nothing
          Resource.unit[IO]
        } else {
          val reporterIO = newState match {
            case AlertState.Pass =>
              // Report once
              reportResolve(ResolveRequest(alert.name))
            case state: AlertState.NotifyState =>
              // Report forever every repeat interval until cancelled.
              IOUtils.timedRepeater(
                repeatEvery = alert.repeatInterval,
                action = reportNotify(NotifyRequest(alert.name, state.message))
              )
          }
          // Returns a resource which will start the reporting in the background.
          reporterIO.background.map(_ => () /* ignore the IO that allows us to know how the fiber ended */)
        }
    } yield SingleExecutionRunResult(newState, resource)

    /** Repeats a single execution forever. */
    def repeatedExecution(
      reporterHotswap: Hotswap[IO, Unit]
    ) = {
      val io = singleExecutionRunRef(alertStateRef).flatMap(reporterHotswap.swap) >> IO.sleep(alert.interval)
      // Acquire a permit to limit max concurrency
      concurrentExecutions.permit
        // Do the IO while holding the permit
        .use(_ => io)
        // Repeat forever
        .foreverM
    }

    // Use the hotswap to ensure previous reporter is stopped when the new one is launched.
    val handler = Hotswap.create[IO, Unit].use(repeatedExecution)
    handler
  }
}
