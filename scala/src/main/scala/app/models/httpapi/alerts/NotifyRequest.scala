package app.models.httpapi.alerts

import play.api.libs.json.{Json, OWrites}

/** Request body for `/notify` endpoint. */
case class NotifyRequest(alertName: AlertName, message: AlertMessage)
object NotifyRequest {
  implicit val writes: OWrites[NotifyRequest] = Json.writes
}