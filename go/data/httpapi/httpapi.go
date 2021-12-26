package httpapi

import (
	. "alert-execution-engine/functional"
	"time"
)

type QueryAlertsResponse struct {
	Alerts []Alert
}

type QueryResponse struct {
	Value QueryValue `json:"value"`
}

// QueryValue is a value returned from a QueryResponse
type QueryValue float64

func (self QueryValue) ToAlertThresholdValue() AlertThresholdValue {
	return AlertThresholdValue(self)
}

// NotifyRequest is a request body for `/notify` endpoint
type NotifyRequest struct {
	AlertName AlertName    `json:"alertName"`
	Message   AlertMessage `json:"message"`
}

// ResolveRequest is a request body for `/resolve` endpoint
type ResolveRequest struct {
	AlertName AlertName `json:"alertName"`
}

// AlertStatusChangeRequest is shared interface between AlertStatusChangeNotifyRequest and ResolveRequest.
type AlertStatusChangeRequest interface {
	ToEither() Either[AlertStatusChangeNotifyRequest, ResolveRequest]
}

func (self *ResolveRequest) ToEither() Either[AlertStatusChangeNotifyRequest, ResolveRequest] {
	return Right[AlertStatusChangeNotifyRequest, ResolveRequest](*self)
}

type AlertStatusChangeNotifyRequest struct {
	Request     NotifyRequest
	RepeatAfter time.Duration
}

func (self *AlertStatusChangeNotifyRequest) ToEither() Either[AlertStatusChangeNotifyRequest, ResolveRequest] {
	return Left[AlertStatusChangeNotifyRequest, ResolveRequest](*self)
}
