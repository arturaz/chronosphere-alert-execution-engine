package app.utils

import play.api.libs.json.Format

import scala.concurrent.duration._

/** Various helper JSON formats. */
object AppFormats {
  /** [[Format]] for [[FiniteDuration]] which is represented as a [[Double]] of seconds. */
  val secondsFiniteDurationFormat: Format[FiniteDuration] =
    implicitly[Format[Double]].bimap(_.seconds, _.toSeconds.toDouble)
}
