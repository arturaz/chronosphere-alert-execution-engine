package app.models.httpapi.alerts

import app.utils.TypeWrapperCompanion
import org.http4s.QueryParamEncoder
import play.api.libs.json.{Json, OFormat, Reads}

/** Name of an alert, is unique and can be used as an identifier. */
case class AlertName(name: String) extends AnyVal
object AlertName extends TypeWrapperCompanion with TypeWrapperCompanion.WithFormat {
  override type Wrapper = AlertName
  override type Underlying = String

  override def getValue(a: AlertName) = a.name
  override def underlyingFormat = implicitly
}

/** Query name that the alert is monitoring. */
case class QueryName(name: String) extends AnyVal
object QueryName extends TypeWrapperCompanion with TypeWrapperCompanion.WithFormat {
  override type Wrapper = QueryName
  override type Underlying = String

  override def getValue(a: QueryName) = a.name
  override def underlyingFormat = implicitly

  implicit val queryParamEncoder: QueryParamEncoder[QueryName] =
    implicitly[QueryParamEncoder[String]].contramap(_.name)
}

/** Message to report when the alert crosses [[AlertThreshold]]. */
case class AlertMessage(message: String) extends AnyVal
object AlertMessage extends TypeWrapperCompanion with TypeWrapperCompanion.WithFormat {
  override type Wrapper = AlertMessage
  override type Underlying = String

  override def getValue(a: AlertMessage) = a.message
  override def underlyingFormat = implicitly
}

/** Threshold above which the respective [[AlertMessage]] should fire. */
case class AlertThresholdValue(value: Double) extends AnyVal
object AlertThresholdValue extends TypeWrapperCompanion with TypeWrapperCompanion.WithFormat {
  override type Wrapper = AlertThresholdValue
  override type Underlying = Double

  override def getValue(a: AlertThresholdValue) = a.value
  override def underlyingFormat = implicitly
}

case class AlertThreshold(message: AlertMessage, value: AlertThresholdValue)
object AlertThreshold {
  /** JSON formatter. */
  implicit val format: OFormat[AlertThreshold] = Json.format
}

/** Success or failure response from the API. */
case class BooleanResponse(success: Boolean)
object BooleanResponse {
  implicit val reads: Reads[BooleanResponse] = ???
}