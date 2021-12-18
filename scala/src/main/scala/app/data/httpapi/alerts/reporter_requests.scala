package app.data.httpapi.alerts

import play.api.libs.json.{Json, OWrites}

/** Requests that the [[app.engine.AlertEngine.reporter]] uses. */
sealed trait ReporterRequest

/** Request body for `/notify` endpoint. */
case class NotifyRequest(alertName: AlertName, message: AlertMessage) extends ReporterRequest
object NotifyRequest {
  implicit val writes: OWrites[NotifyRequest] = Json.writes
}

/** Request body for `/resolve` endpoint. */
case class ResolveRequest(alertName: AlertName) extends ReporterRequest
object ResolveRequest {
  implicit val writes: OWrites[ResolveRequest] = Json.writes
}