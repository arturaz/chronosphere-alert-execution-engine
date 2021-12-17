package app.models.httpapi.alerts

import play.api.libs.json.{Json, OWrites}

/** Request body for `/resolve` endpoint. */
case class ResolveRequest(alertName: AlertName)
object ResolveRequest {
  implicit val writes: OWrites[ResolveRequest] = Json.writes
}
