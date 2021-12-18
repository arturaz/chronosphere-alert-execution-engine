package app.utils

import play.api.libs.json.Format

/** Utilities for types that wrap an [[TypeWrapperCompanion#Underlying]] type as [[TypeWrapperCompanion#Wrapper]]. */
trait TypeWrapperCompanion {
  type Wrapper
  type Underlying

  /** Creates new [[Wrapper]] from [[Underlying]]. */
  def apply(value: Underlying): Wrapper

  /** Extracts [[Underlying]] from [[Wrapper]]. */
  def getValue(a: Wrapper): Underlying

  implicit def toUnderlying(value: Wrapper): Underlying = getValue(value)
}
object TypeWrapperCompanion {
  /** Adds [[Format]] to the [[TypeWrapperCompanion]]. */
  trait WithFormat { _: TypeWrapperCompanion =>
    def underlyingFormat: Format[Underlying]

    implicit val format: Format[Wrapper] = underlyingFormat.bimap(apply, getValue)
  }

  /** Adds [[Ordering]] to the [[TypeWrapperCompanion]]. */
  trait WithOrdering { _: TypeWrapperCompanion =>
    def underlyingOrdering: Ordering[Underlying]

    implicit val ordering: Ordering[Wrapper] = Ordering.by(getValue)(underlyingOrdering)
  }
}