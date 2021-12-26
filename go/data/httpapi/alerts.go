package httpapi

import (
	"alert-execution-engine/data"
	. "alert-execution-engine/functional"
	"time"
)

type AlertName string

func (a AlertName) String() string {
	return string(a)
}

type QueryName string

func (q QueryName) String() string {
	return string(q)
}

type AlertMessage string
type AlertThresholdValue float64

type Alert struct {
	Name           AlertName
	Query          QueryName
	Interval       time.Duration
	RepeatInterval time.Duration
	Thresholds     AlertThresholds
}

// JsonAlert is Alert which we parse from JSON.
type JsonAlert struct {
	Name           AlertName         `json:"name"`
	Query          QueryName         `json:"query"`
	Interval       data.DurationSecs `json:"intervalSecs"`
	RepeatInterval data.DurationSecs `json:"repeatIntervalSecs"`
	Warn           AlertThreshold    `json:"warn"`
	Critical       AlertThreshold    `json:"critical"`
}

func (self *JsonAlert) toAlert() Alert {
	return Alert{
		Name:           self.Name,
		Query:          self.Query,
		Interval:       self.Interval.ToDuration(),
		RepeatInterval: self.RepeatInterval.ToDuration(),
		Thresholds: AlertThresholds{
			Warn:     self.Warn,
			Critical: self.Critical,
		},
	}
}

type AlertThreshold struct {
	Message AlertMessage        `json:"message"`
	Value   AlertThresholdValue `json:"value"`
}

//region AlertThresholds

type AlertThresholds struct {
	Warn     AlertThreshold
	Critical AlertThreshold
}

func (self AlertThresholds) StateFor(value AlertThresholdValue) AlertState {
	if value >= self.Critical.Value {
		return &AlertStateCritical{self.Critical.Message}
	} else if value >= self.Warn.Value {
		return &AlertStateWarn{self.Warn.Message}
	} else {
		return &AlertStatePass{}
	}
}

//endregion

type AlertState interface {
	// NotifyMessage - when None, `/resolve` should be called, if Some, `/notify` should be called.
	NotifyMessage() Option[AlertMessage]
}

//region AlertStatePass

type AlertStatePass struct{}

func (self *AlertStatePass) NotifyMessage() Option[AlertMessage] {
	return None[AlertMessage]()
}

//endregion

//region AlertStateWarn

type AlertStateWarn struct {
	message AlertMessage
}

func (self *AlertStateWarn) NotifyMessage() Option[AlertMessage] {
	return Some(self.message)
}

//endregion

//region AlertStateCritical

type AlertStateCritical struct {
	message AlertMessage
}

func (self *AlertStateCritical) NotifyMessage() Option[AlertMessage] {
	return Some(self.message)
}

//endregion
