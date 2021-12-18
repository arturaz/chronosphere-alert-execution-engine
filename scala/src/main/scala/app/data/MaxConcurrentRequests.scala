package app.data

import app.utils.TypeWrapperCompanion

/** Maximum number of requests that can happen concurrently. */
case class MaxConcurrentRequests private (value: Long) extends AnyVal
object MaxConcurrentRequests extends TypeWrapperCompanion {
  override type Wrapper = MaxConcurrentRequests
  override type Underlying = Long

  /** Extracts [[Underlying]] from [[Wrapper]]. */
  override def getValue(a: MaxConcurrentRequests) = a.value

  def create(value: Long): Either[String, MaxConcurrentRequests] =
    if (value >= 1) Right(apply(value))
    else Left(s"MaxConcurrentRequests needs to be at least 1, $value given")
}