package engine

import (
	"alert-execution-engine/data/httpapi"
	. "alert-execution-engine/functional"
	"alert-execution-engine/utils"
	"go.uber.org/zap"
	"sync"
	"time"
)

// DoNotify takes in isAborted because we need to communicate to the HTTP api that we should stop retrying sending a
// request. If it fails, the reporter stops.
type DoNotify func(isAborted utils.IsChannelClosed, request httpapi.NotifyRequest) error

// DoResolve - same as DoNotify.
type DoResolve func(isAborted utils.IsChannelClosed, request httpapi.ResolveRequest) error

func Initialize[Err error](
	log zap.Logger,
	// This function could be a generic type, but Goland freaks out :)
	queryAlerts func() (*httpapi.QueryAlertsResponse, *Err),
	// DoQuery can fail, if it does, the alert engine for a particular alert will die.
	doQuery func(name httpapi.QueryName) (*httpapi.QueryResponse, *Err),
	doNotify DoNotify,
	doResolve DoResolve,
) *Err {
	log.Info("Querying alerts...")
	alerts, err := queryAlerts()
	if err != nil {
		return err
	}
	log.Info("Alerts queried.", zap.Any("alerts", alerts))

	waitGroup := new(sync.WaitGroup)
	waitGroup.Add(len(alerts.Alerts))
	for _, alert := range alerts.Alerts {
		go InitializeAlerts(log, waitGroup, alert, doQuery, doNotify, doResolve)
	}

	log.Info("Waiting until all alert engines finish or error out.")
	waitGroup.Wait()

	return nil
}

func InitializeAlerts[Err error](
	log zap.Logger,
	waitGroup *sync.WaitGroup,
	alert httpapi.Alert,
	// DoQuery can fail, if it does, the alert engine for a particular alert will die.
	doQuery func(name httpapi.QueryName) (*httpapi.QueryResponse, *Err),
	doNotify DoNotify,
	doResolve DoResolve,
) *Err {
	// If we error out, notify the master goroutine.
	defer waitGroup.Done()

	alertLogField := zap.String("alert", alert.Name.String())
	log.Info("Starting engine.", alertLogField)

	var state httpapi.AlertState = &httpapi.AlertStatePass{}
	var maybeReporter = None[RunningReporter]()

	for {
		log.Debug("Querying", alertLogField)
		response, err := doQuery(alert.Query)
		if err != nil {
			log.Error("Querying failed", alertLogField, zap.Error(*err))
			return err
		}
		log.Debug("Received response", alertLogField, zap.Any("response", response))
		newState := alert.Thresholds.StateFor(response.Value.ToAlertThresholdValue())
		if state == newState {
			log.Debug("State did not change", alertLogField)
		} else {
			log.Info("State changed", alertLogField, zap.Any("from", state), zap.Any("to", newState))
			state = newState

			maybeReporter.IfSome(func(reporter RunningReporter) {
				log.Debug("Detected previous reporter, stopping it.", alertLogField)
				reporter.abort()
			})
			statusChangeRequest := Fold[httpapi.AlertMessage, httpapi.AlertStatusChangeRequest](
				state.NotifyMessage(),
				func() httpapi.AlertStatusChangeRequest { return &httpapi.ResolveRequest{AlertName: alert.Name} },
				func(message httpapi.AlertMessage) httpapi.AlertStatusChangeRequest {
					return &httpapi.AlertStatusChangeNotifyRequest{
						Request: httpapi.NotifyRequest{
							AlertName: alert.Name,
							Message:   message,
						},
						RepeatAfter: alert.RepeatInterval,
					}
				},
			)
			maybeReporter = Some(InitializeAlertReporter(log, alert.Name, statusChangeRequest, doNotify, doResolve))

			log.Debug("Sleeping...", alertLogField, zap.Duration("for", alert.Interval))
			time.Sleep(alert.Interval)
		}
	}
}

type RunningReporter struct {
	// Abort the reporter goroutine by closing this channel
	abortChannel chan Unit
}

func (self *RunningReporter) abort() {
	close(self.abortChannel)
}

func InitializeAlertReporter[Err any](
	log zap.Logger,
	name httpapi.AlertName,
	request httpapi.AlertStatusChangeRequest,
	doNotify DoNotify,
	doResolve DoResolve,
) RunningReporter {
	abortChannel := make(chan Unit)
	isAborted := utils.ProduceCheckIfChanIsClosedFunc(abortChannel)
	go alertReporterMain(
		log, name, request,
		func(request httpapi.NotifyRequest) error { return doNotify(isAborted, request) },
		func(request httpapi.ResolveRequest) error { return doResolve(isAborted, request) },
	)

	return RunningReporter{abortChannel: abortChannel}
}

func alertReporterMain(
	log zap.Logger,
	name httpapi.AlertName,
	request httpapi.AlertStatusChangeRequest,
	doNotify func(request httpapi.NotifyRequest) error,
	doResolve func(request httpapi.ResolveRequest) error,
) {
	alertLogField := zap.String("alert-reporter", name.String())

	request.ToEither().Match(
		func(req httpapi.AlertStatusChangeNotifyRequest) {
			// Invoke doNotify, then repeat every interval.
			for {
				log.Debug("Sending notify", alertLogField, zap.Any("request", req.Request))
				err := doNotify(req.Request)
				if err != nil {
					log.Error("Notify failed!", alertLogField, zap.Error(err))
					return
				}
				log.Debug(
					"Sleeping until next notify", alertLogField, zap.Duration("sleepFor", req.RepeatAfter),
				)
				time.Sleep(req.RepeatAfter)
			}
		},
		func(req httpapi.ResolveRequest) {
			// Invoke the resolve once.
			err := doResolve(req)
			if err != nil {
				log.Error("Resolve failed!", alertLogField, zap.Error(err))
			}
		},
	)
}
