package data

import "time"

// DurationSecs is a time.Duration in seconds.
type DurationSecs int64

func (self *DurationSecs) ToDuration() time.Duration {
	return time.Duration(int64(*self) * int64(time.Second))
}
