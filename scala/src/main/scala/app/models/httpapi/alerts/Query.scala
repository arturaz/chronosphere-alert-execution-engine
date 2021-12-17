package app.models.httpapi.alerts

import app.utils.TypeWrapperCompanion
import play.api.libs.json.{Json, OFormat}

/** A value returned from [[Query]]. */
case class QueryValue(value: Double) extends AnyVal
object QueryValue extends TypeWrapperCompanion[QueryValue, Double] {
  override def getValue(a: QueryValue) = a.value
}

/** Request body for the `/query` endpoint. */
case class Query(value: QueryValue)
object Query {
  implicit val format: OFormat[Query] = Json.format
}
