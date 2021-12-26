package utils

import (
	"time"
)

type BackoffSchedule struct {
	schedule   []time.Duration
	currentIdx uint
}

// Advance returns the current time.Duration and moves the currentIdx by one.
func (self *BackoffSchedule) Advance() time.Duration {
	duration := self.schedule[self.currentIdx]
	self.currentIdx += 1
	if self.currentIdx >= uint(len(self.schedule)) {
		self.currentIdx = uint(len(self.schedule) - 1)
	}
	return duration
}

func BackoffScheduleNewDefault() BackoffSchedule {
	return BackoffSchedule{
		schedule: []time.Duration{
			100 * time.Millisecond, 250 * time.Millisecond, 500 * time.Millisecond, 1 * time.Second, 2 * time.Second,
		},
		currentIdx: 0,
	}
}
