package app.data.httpapi.alerts

import app.utils.{AppFormats, TypeWrapperCompanion}
import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Json, OFormat}

import scala.concurrent.duration.FiniteDuration
import scala.math.Ordering.Implicits._


/**
 * @param interval how often the alert should execute
 * @param repeatInterval how often should messages be re-sent for an alert that is violating thresholds
 */
case class Alert(
  name: AlertName, query: QueryName, interval: FiniteDuration, repeatInterval: FiniteDuration,
  thresholds: AlertThresholds
) {
  def debugString: String =
    s"""### Alert '${name.name}'
       |$query
       |interval = $interval, repeat interval = $repeatInterval
       |${thresholds.debugString}
       |""".stripMargin
}
object Alert {
  implicit val format: OFormat[Alert] = (
    (JsPath \ "name").format[AlertName] and
    (JsPath \ "query").format[QueryName] and
    (JsPath \ "intervalSecs").format(AppFormats.secondsFiniteDurationFormat) and
    (JsPath \ "repeatIntervalSecs").format(AppFormats.secondsFiniteDurationFormat) and
    (
      (JsPath \ "warn").format[AlertThreshold] and
      (JsPath \ "critical").format[AlertThreshold]
    )(AlertThresholds.apply, unlift(AlertThresholds.unapply))
  )(apply, unlift(unapply))
}

sealed trait AlertState
object AlertState {
  /** States for which the [[NotifyRequest]] should be sent. */
  sealed trait NotifyState extends AlertState {
    def message: AlertMessage
  }

  /** [[AlertThresholdValue]] <= [[AlertThresholds.warn]] */
  case object Pass extends AlertState
  /** [[AlertThresholdValue]] > [[AlertThresholds.warn]] and [[AlertThresholdValue]] <= [[AlertThresholds.critical]] */
  case class Warn(message: AlertMessage) extends NotifyState
  /** [[AlertThresholdValue]] > [[AlertThresholds.critical]] */
  case class Critical(message: AlertMessage) extends NotifyState
}


/** Name of an alert, is unique and can be used as an identifier. */
case class AlertName(name: String) extends AnyVal
object AlertName extends TypeWrapperCompanion with TypeWrapperCompanion.WithFormat {
  override type Wrapper = AlertName
  override type Underlying = String

  override def getValue(a: AlertName) = a.name
  override def underlyingFormat = implicitly
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
object AlertThresholdValue extends TypeWrapperCompanion
  with TypeWrapperCompanion.WithFormat with TypeWrapperCompanion.WithOrdering
{
  override type Wrapper = AlertThresholdValue
  override type Underlying = Double

  override def getValue(a: AlertThresholdValue) = a.value
  override def underlyingFormat = implicitly
  override def underlyingOrdering = implicitly
}

case class AlertThreshold(message: AlertMessage, value: AlertThresholdValue)
object AlertThreshold {
  /** JSON formatter. */
  implicit val format: OFormat[AlertThreshold] = Json.format
}

case class AlertThresholds(warn: AlertThreshold, critical: AlertThreshold) {
  def debugString: String =
    s"""warn     @ ${warn.value} -> ${warn.message}
       |critical @ ${critical.value} -> ${critical.message}
       |""".stripMargin

  def stateFor(value: AlertThresholdValue): AlertState = {
    if (value > critical.value) AlertState.Critical(critical.message)
    else if (value > warn.value) AlertState.Warn(warn.message)
    else AlertState.Pass
  }
}