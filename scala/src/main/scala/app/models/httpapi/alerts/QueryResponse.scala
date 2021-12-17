package app.models.httpapi.alerts

import app.utils.TypeWrapperCompanion
import play.api.libs.json.{Json, OFormat}

/** A value returned from [[QueryResponse]]. */
case class QueryValue(value: Double) extends AnyVal
object QueryValue extends TypeWrapperCompanion with TypeWrapperCompanion.WithFormat {
  override type Wrapper = QueryValue
  override type Underlying = Double

  override def getValue(a: QueryValue) = a.value
  override def underlyingFormat = implicitly
}

/** Response body for the `/query` endpoint. */
case class QueryResponse(value: QueryValue)
object QueryResponse {
  implicit val format: OFormat[QueryResponse] = Json.format
}
