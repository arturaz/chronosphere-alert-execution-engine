package app.data

import app.utils.TypeWrapperCompanion

/** Maximum number of alert executions that can happen concurrently. */
case class MaxConcurrentAlertExecutions private (value: Long) extends AnyVal
object MaxConcurrentAlertExecutions extends TypeWrapperCompanion {
  override type Wrapper = MaxConcurrentAlertExecutions
  override type Underlying = Long

  /** Extracts [[Underlying]] from [[Wrapper]]. */
  override def getValue(a: MaxConcurrentAlertExecutions) = a.value

  def create(value: Long): Either[String, MaxConcurrentAlertExecutions] =
    if (value >= 1) Right(apply(value))
    else Left(s"MaxConcurrentAlertExecutions needs to be at least 1, $value given")
}