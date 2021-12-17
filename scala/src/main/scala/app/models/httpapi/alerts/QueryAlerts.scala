package app.models.httpapi.alerts

import app.utils.AppFormats
import play.api.libs.functional.syntax._
import play.api.libs.json.{Format, JsPath, OFormat}

import scala.concurrent.duration.FiniteDuration

/**
 * @param interval how often the alert should execute
 * @param repeatInterval how often should messages be re-sent for an alert that is violating thresholds
 */
case class QueryAlert(
  name: AlertName, query: QueryName, interval: FiniteDuration, repeatInterval: FiniteDuration,
  warn: AlertThreshold, critical: AlertThreshold
)
object QueryAlert {
  implicit val format: OFormat[QueryAlert] = (
    (JsPath \ "name").format[AlertName] and
    (JsPath \ "query").format[QueryName] and
    (JsPath \ "intervalSecs").format(AppFormats.secondsFiniteDurationFormat) and
    (JsPath \ "repeatIntervalSecs").format(AppFormats.secondsFiniteDurationFormat) and
    (JsPath \ "warn").format[AlertThreshold] and
    (JsPath \ "critical").format[AlertThreshold]
  )(apply, unlift(unapply))
}

case class QueryAlerts(formats: Vector[QueryAlert])
object QueryAlerts {
  implicit val format: Format[QueryAlerts] =
    implicitly[Format[Vector[QueryAlert]]].bimap(apply, _.formats)
}
