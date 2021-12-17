package app

import cats.effect.{IO, IOApp}
import org.http4s.blaze.client.BlazeClientBuilder

object Main extends IOApp {
  /** Entry point. */
  override def run(args: List[String]) = ???

  val client = BlazeClientBuilder[IO].resource.use { client =>
    // use `client` here and return an `IO`.
    // the client will be acquired and shut down
    // automatically each time the `IO` is run.
    IO.unit
  }
}
