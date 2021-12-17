package app.models.httpapi.alerts

import app.utils.TypeWrapperCompanion
import play.api.libs.json.{Json, OFormat}

/** Name of an alert, is unique and can be used as an identifier. */
case class AlertName(name: String) extends AnyVal
object AlertName extends TypeWrapperCompanion[AlertName, String] {
  override def getValue(a: AlertName) = a.name
}

/** Query name that the alert is monitoring. */
case class QueryName(name: String) extends AnyVal
object QueryName extends TypeWrapperCompanion[QueryName, String] {
  override def getValue(a: QueryName) = a.name
}

/** Message to report when the alert crosses [[AlertThreshold]]. */
case class AlertMessage(message: String) extends AnyVal
object AlertMessage extends TypeWrapperCompanion[AlertMessage, String] {
  override def getValue(a: AlertMessage) = a.message
}

/** Threshold above which the respective [[AlertMessage]] should fire. */
case class AlertThresholdValue(value: Double) extends AnyVal
object AlertThresholdValue extends TypeWrapperCompanion[AlertThresholdValue, Double] {
  override def getValue(a: AlertThresholdValue) = a.value
}

case class AlertThreshold(message: AlertMessage, value: AlertThresholdValue)
object AlertThreshold {
  /** JSON formatter. */
  implicit val format: OFormat[AlertThreshold] = Json.format
}