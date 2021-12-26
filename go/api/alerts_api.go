package api

import (
	"alert-execution-engine/data/httpapi"
	. "alert-execution-engine/functional"
	"context"
	"encoding/json"
	"fmt"
	"golang.org/x/sync/semaphore"
	"net/http"
	"net/url"
)
import HttpClient "github.com/bozd4g/go-http-client"

type Client struct {
	BaseUri                 url.URL
	maybeConcurrentRequests *semaphore.Weighted
	client                  HttpClient.Client
}

type MaxConcurrentRequests uint

func NewClient(baseUri url.URL, maxConcurrentRequests Option[MaxConcurrentRequests]) Client {
	var concurrentRequests *semaphore.Weighted
	if maxConcurrentRequests.IsSome {
		concurrentRequests = semaphore.NewWeighted(int64(maxConcurrentRequests.UnsafeValue))
	}

	return Client{
		BaseUri:                 baseUri,
		maybeConcurrentRequests: concurrentRequests,
		client:                  HttpClient.New(baseUri.String()),
	}
}

func (self *Client) QueryAlerts() (*httpapi.QueryAlertsResponse, error) {
	request, err := self.client.Get("/alerts")
	if err != nil {
		return nil, err
	}

	arr, err := sendAndParse[[]httpapi.Alert](self, request)
	if err != nil {
		return nil, err
	}

	return &httpapi.QueryAlertsResponse{Alerts: *arr}, nil
}

func (self *Client) Query(queryName httpapi.QueryName) (*httpapi.QueryResponse, error) {
	queryParams := struct {
		Target string `url:"target"`
	}{Target: queryName.String()}
	request, err := self.client.GetWith("/query", queryParams)
	if err != nil {
		return nil, err
	}

	return sendAndParse[httpapi.QueryResponse](self, request)
}

func (self *Client) Notify(requestData httpapi.NotifyRequest) error {
	request, err := self.client.PostWith("/notify", requestData)
	if err != nil {
		return err
	}

	_, err = self.sendAndFailOnNonSuccess(request)
	return err
}

func (self *Client) Resolve(requestData httpapi.ResolveRequest) error {
	request, err := self.client.PostWith("/resolve", requestData)
	if err != nil {
		return err
	}

	_, err = self.sendAndFailOnNonSuccess(request)
	return err
}

type BodyParsingError struct {
	Body         []byte
	ParsingError error
}

func (self BodyParsingError) Error() string {
	return fmt.Sprintf("Cannot parse JSON: %s\nJSON:\n%s", self.ParsingError, string(self.Body))
}

func sendAndParse[Parsed any](self *Client, request *http.Request) (*Parsed, error) {
	response, err := self.send(request)
	if err != nil {
		return nil, err
	}

	body := response.Get().Body
	var parsed Parsed
	err = json.Unmarshal(body, &parsed)
	if err != nil {
		return nil, &BodyParsingError{
			Body:         body,
			ParsingError: err,
		}
	}

	return &parsed, nil
}

type NonSuccessfulRequestError struct {
	Response HttpClient.Response
}

func (self NonSuccessfulRequestError) Error() string {
	return fmt.Sprintf("Response status code %d is not in the 2xx range", self.Response.Get().StatusCode)
}

// Sends the request, fails if the response does not have 2xx status code.
func (self *Client) sendAndFailOnNonSuccess(request *http.Request) (HttpClient.Response, error) {
	response, err := self.send(request)
	if err != nil {
		return nil, err
	}

	statusCode := response.Get().StatusCode
	if statusCode < 200 || statusCode > 299 {
		return nil, NonSuccessfulRequestError{Response: response}
	}

	return response, nil
}

func (self *Client) send(request *http.Request) (HttpClient.Response, error) {
	if self.maybeConcurrentRequests != nil {
		ctx := context.TODO()
		err := self.maybeConcurrentRequests.Acquire(ctx, 1)
		if err != nil {
			return nil, err
		}
		defer self.maybeConcurrentRequests.Release(1)
	}
	return self.client.Do(request)
}
