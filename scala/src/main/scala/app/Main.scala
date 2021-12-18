package app

import app.api.AlertsAPI
import app.data.MaxConcurrentAlertExecutions
import app.data.httpapi.alerts._
import app.engine.AlertEngine
import app.utils.IOUtils
import cats.effect.std.Console
import cats.syntax.either._
import cats.syntax.apply._
import cats.effect.{ExitCode, IO, IOApp}
import com.emarsys.logger.ce.CatsEffectLogging
import org.http4s.Uri
import org.http4s.blaze.client.BlazeClientBuilder

import scala.concurrent.duration.DurationInt

object Main extends IOApp {
  /** Entry point. */
  override def run(args: List[String]) = {
    val console = Console[IO]
    args match {
      case "test-engine" :: Nil =>
        testEngine.as(ExitCode.Success)
      case baseUriStr :: maxConcurrentAlertExecutionsStr :: Nil =>
        val baseUriValidated =
          Uri.fromString(baseUriStr)
            .left.map(err => s"Can't parse '$baseUriStr' as uri: $err")
            .map(AlertsAPI.BaseUri.apply)
            .toValidatedNec
        val maxConcurrentAlertExecutionsValidated =
          maxConcurrentAlertExecutionsStr.toLongOption
            .toRight(s"'$maxConcurrentAlertExecutionsStr' is not a number")
            .flatMap(MaxConcurrentAlertExecutions.create).toValidatedNec

        (baseUriValidated, maxConcurrentAlertExecutionsValidated).mapN(actualEngine).fold(
          errs =>
            console.errorln(s"Command line argument errors found:\n${errs.iterator.mkString("\n")}").as(ExitCode.Error),
          runEngine =>
            console.println("Starting the engine...") >> runEngine >> console.println("Engine shut down.") >>
              IO.pure(ExitCode.Success)
        )
      case _ =>
        console
          .errorln(
            """Usage: ./program base_alerts_api_uri max_concurrent_alert_executions
              |
              |or: ./program test-engine""".stripMargin)
          .as(ExitCode.Error)
    }
  }

  def actualEngine(
    baseUri: AlertsAPI.BaseUri, maxConcurrentAlertExecutions: MaxConcurrentAlertExecutions
  ): IO[Unit] = {
    implicit val implicitBaseUri = baseUri
    BlazeClientBuilder[IO].resource.use { implicit client =>
      CatsEffectLogging.createEffectLogger[IO]("AlertEngine").flatMap { implicit logging =>
        import IOUtils.repeatUntilSuccessful

        AlertEngine.initialize(
          fetchQueries = repeatUntilSuccessful(AlertsAPI.queryAlerts[IO]),
          fetchQueryValue = queryName => repeatUntilSuccessful(AlertsAPI.query[IO](queryName)).map(_.value),
          reportNotify = request => repeatUntilSuccessful(AlertsAPI.notify[IO](request).flatMap(_.asEffect[IO])),
          reportResolve = request => repeatUntilSuccessful(AlertsAPI.resolve[IO](request).flatMap(_.asEffect[IO])),
          maxConcurrentAlertExecutions
        )
      }
    }
  }

  def testEngine: IO[Unit] =
    CatsEffectLogging.createEffectLogger[IO]("TestAlertEngine").flatMap { implicit logging =>
      AlertEngine.initialize(
        fetchQueries = IO.pure(QueryAlertsResponse(Vector.tabulate(5)(i =>
          Alert(AlertName(s"a#$i"), QueryName(s"q#$i"), 10.seconds, 20.seconds, AlertThresholds(
            AlertThreshold(AlertMessage(s"WARN $i"), AlertThresholdValue(i * 40)),
            AlertThreshold(AlertMessage(s"CRIT $i"), AlertThresholdValue(i * 80)),
          ))
        ))),
        fetchQueryValue = name => {
          val value = QueryValue(name.hashCode() % 1000)
          IO.println(s"Fetching query value for $name: $value") >> IO.pure(value)
        },
        reportNotify = req => IO.println(s"Reporting $req...") >> IO.sleep(3.seconds) >> IO.println(s"Reported $req."),
        reportResolve = req => IO.println(s"Reporting $req...") >> IO.sleep(3.seconds) >> IO.println(s"Reported $req."),
        maxConcurrentAlertExecutions = MaxConcurrentAlertExecutions(2)
      )
    }
}
