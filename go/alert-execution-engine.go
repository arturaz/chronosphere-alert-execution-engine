package main

import (
	"alert-execution-engine/api"
	"alert-execution-engine/data/httpapi"
	"alert-execution-engine/engine"
	. "alert-execution-engine/functional"
	"alert-execution-engine/utils"
	"fmt"
	"go.uber.org/zap"
	"net/url"
)

func main() {
	log, err := zap.NewDevelopment()
	if err != nil {
		panicStr(fmt.Sprintf("Error initializing logger: %s", err))
	}

	baseUri, err := url.Parse("http://localhost:9001")
	if err != nil {
		panicStr(fmt.Sprintf("Can't parse base url: %s", err))
	}

	client := api.NewClient(*baseUri, Some(api.MaxConcurrentRequests(3)))

	engine.Initialize(
		log,
		func() (*httpapi.QueryAlertsResponse, *error) {
			return client.QueryAlerts()
		},
		func(name httpapi.QueryName) (*httpapi.QueryResponse, *error) {
			return client.Query(name)
		},
		func(isAborted utils.IsChannelClosed, request httpapi.NotifyRequest) error {

		},
	)
}

func panicStr(s string) {
	// For some reason goland says 'Cannot use 's' (type string) as the type any', which isn't true, but by defining
	// this function we at least have one place where this error is shown instead of every place where we want to panic
	// with a string.
	panic(s)
}
