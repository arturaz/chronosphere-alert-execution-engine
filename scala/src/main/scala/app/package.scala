import cats.data.NonEmptyList

package object app {
  implicit class AnyExts[A](private val value: A) extends AnyVal {
    /** Thrush combinator. */
    def |>[B](f: A => B): B = f(value)
  }

  implicit class NonEmptyListExts[A](private val neList: NonEmptyList[A]) extends AnyVal {
    /** Returns a [[NonEmptyList]] without the head element or same list if there's only one element left. */
    def nonEmptyTail: NonEmptyList[A] = NonEmptyList.fromList(neList.tail) match {
      case None => neList
      case Some(list) => list
    }
  }
}
