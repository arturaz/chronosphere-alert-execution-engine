package app.utils

import play.api.libs.json.Format

/** Utilities for types that wrap an [[Underlying]] type as [[Wrapper]]. */
abstract class TypeWrapperCompanion[Wrapper, Underlying : Format] {
  /** Creates new [[Wrapper]] from [[Underlying]]. */
  def apply(value: Underlying): Wrapper

  /** Extracts [[Underlying]] from [[Wrapper]]. */
  def getValue(a: Wrapper): Underlying

  implicit val format: Format[Wrapper] = implicitly[Format[Underlying]].bimap(apply, getValue)
}