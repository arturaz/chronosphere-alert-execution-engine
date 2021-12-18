package app.utils

import app.NonEmptyListExts
import cats.data.NonEmptyList
import cats.effect.IO

import scala.concurrent.duration._

object IOUtils {
  /** Defines time intervals for repeating actions. */
  case class BackoffSchedule(schedule: NonEmptyList[FiniteDuration]) {
    /** Pops one element from the front of the list, going to the next timeout. */
    def shift: (FiniteDuration, BackoffSchedule) = (schedule.head, BackoffSchedule(schedule.nonEmptyTail))
  }
  object BackoffSchedule {
    val Default = apply(NonEmptyList(100.millis, List(200.millis, 500.millis, 750.millis, 1.second)))
  }

  trait OnIOFailure {
    def apply(error: Throwable, willRetryAfter: FiniteDuration): IO[Unit]
  }
  object OnIOFailure {
    val DoNothing: OnIOFailure = (_, _) => IO.unit
  }

  /**
   * Invokes `createIO` and runs the [[IO]]. If it fails invokes [[OnIOFailure]] and retries it according to
   * [[BackoffSchedule]].
   **/
  def repeatUntilSuccessfulLazy[A](
    createIO: () => IO[A], onFailure: OnIOFailure = OnIOFailure.DoNothing,
    schedule: BackoffSchedule=BackoffSchedule.Default
  ): IO[A] = {
    createIO().redeemWith(
      recover = throwable => {
        val (retryAfter, newSchedule) = schedule.shift
        onFailure(throwable, retryAfter) *> repeatUntilSuccessfulLazy(createIO, onFailure, newSchedule)
      },
      IO.pure
    )
  }

  /** Version of [[repeatUntilSuccessfulLazy]] which takes it's [[IO]] eagerly. */
  def repeatUntilSuccessful[A](
    io: IO[A], onFailure: OnIOFailure = OnIOFailure.DoNothing,
    schedule: BackoffSchedule=BackoffSchedule.Default
  ): IO[A] =
    repeatUntilSuccessfulLazy(() => io, onFailure, schedule)

  /** Indefinitely repeats the given `action` every [[FiniteDuration]]. */
  def timedRepeater(repeatEvery: FiniteDuration, action: IO[Unit]): IO[Nothing] =
    (action *> IO.sleep(repeatEvery)).foreverM

  /**
   * Indefinitely repeats the given `action` every [[FiniteDuration]], passing in a repetition index each time.
   * Index starts from 0.
   **/
  def timedCountingRepeater(repeatEvery: FiniteDuration, action: Int => IO[Unit]): IO[Nothing] = {
    def rec(index: Int): IO[Nothing] = (action(index) *> IO.sleep(repeatEvery)) >> rec(index + 1)
    rec(0)
  }
}
