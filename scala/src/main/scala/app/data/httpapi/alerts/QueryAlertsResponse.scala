package app.data.httpapi.alerts

import play.api.libs.json.Format


case class QueryAlertsResponse(alerts: Vector[Alert])
object QueryAlertsResponse {
  implicit val format: Format[QueryAlertsResponse] =
    implicitly[Format[Vector[Alert]]].bimap(apply, _.alerts)
}
