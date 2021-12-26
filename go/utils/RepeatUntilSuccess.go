package utils

import (
	"fmt"
	"go.uber.org/zap"
	"time"
)

// RepeatUntilSuccess runs the given function `f`, repeating it with BackoffSchedule until it stops returning an Error.
func RepeatUntilSuccess[Result any, Error any](
	schedule BackoffSchedule,
	f func() (*Result, *Error),
	keepTrying func() bool,
	debugName string,
	log zap.Logger,
) *Result {
	result, err := f()
	for err != nil {
		if keepTrying() {
			sleepFor := schedule.Advance()
			log.Debug(
				fmt.Sprintf("Failed to run %s, sleeping before retrying", debugName),
				zap.Any("err", err),
				zap.Duration("retryIn", sleepFor),
			)
			time.Sleep(sleepFor)
			result, err = f()
		} else {
			log.Debug(
				fmt.Sprintf("Failed to run %s, not retrying anymore", debugName),
				zap.Any("err", err),
			)
			return nil
		}
	}

	return result
}
