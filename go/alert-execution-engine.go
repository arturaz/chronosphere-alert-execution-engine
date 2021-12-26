package main

import (
	"alert-execution-engine/api"
	"alert-execution-engine/data/httpapi"
	"fmt"
	"net/url"
)

func main() {
	baseUri, err := url.Parse("http://localhost:9001")
	if err != nil {
		panicStr(fmt.Sprintf("Can't parse base url: %s", err))
	}

	client := api.NewClient(*baseUri)

	alerts, err := client.QueryAlerts()
	if err != nil {
		panicStr(fmt.Sprintf("Can't get query alerts: %s", err))
	}

	fmt.Println(alerts)

	response, err := client.Query(alerts.Alerts[0].Query)
	if err != nil {
		panicStr(fmt.Sprintf("Can't query: %s", err))
	}
	fmt.Println(response)

	err = client.Notify(httpapi.NotifyRequest{
		AlertName: alerts.Alerts[0].Name,
		Message:   "Hello",
	})
	if err != nil {
		panicStr(fmt.Sprintf("Can't notify: %s", err))
	}

	err = client.Resolve(httpapi.ResolveRequest{
		AlertName: alerts.Alerts[0].Name,
	})
	if err != nil {
		panicStr(fmt.Sprintf("Can't resolve: %s", err))
	}
}

func panicStr(s string) {
	// For some reason goland says 'Cannot use 's' (type string) as the type any', which isn't true, but by defining
	// this function we at least have one place where this error is shown instead of every place where we want to panic
	// with a string.
	panic(s)
}
