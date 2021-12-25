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

  implicit class StringExts(private val str: String) extends AnyVal {
    /** Backport of https://docs.oracle.com/en/java/javase/12/docs/api/java.base/java/lang/String.html#indent(int) */
    def indent(n: Int): String = {
      val indenter = " " * n
      str.split("\n").iterator.map(indenter + _).mkString("\n")
    }
  }
}
