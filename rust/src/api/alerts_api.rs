use std::fmt::Debug;
use std::future::Future;
use std::pin::Pin;
use std::sync::Arc;
use futures_util::future;
use log::debug;
use reqwest::{RequestBuilder, Response, Url};
use tokio::sync::Semaphore;
use crate::data::httpapi::{NotifyRequest, QueryAlertsResponse, QueryResponse, ResolveRequest};
use crate::data::httpapi::alerts::QueryName;

#[derive(Clone)]
pub struct BaseUri(pub Url);

pub struct MaxConcurrentRequests(pub usize);

#[derive(Clone)]
pub struct Client {
    base_uri: BaseUri,
    max_concurrent_requests: Arc<Semaphore>,
    client: reqwest::Client
}

impl Client {
    pub fn new(base_uri: BaseUri, max_concurrent_requests: MaxConcurrentRequests) -> Client {
        Client {
            base_uri,
            max_concurrent_requests: Arc::new(Semaphore::new(max_concurrent_requests.0)),
            client: reqwest::Client::new()
        }
    }

    pub fn query_alerts(&self) -> impl Future<Output = reqwest::Result<QueryAlertsResponse>> {
        let url = self.base_uri.0.join("alerts").unwrap();
        let request = self.client.get(url);
        self.do_request(
            request,
            |response| Box::pin(response.json::<QueryAlertsResponse>())
        )
    }

    pub fn query(
        &self, query_name: &QueryName
    ) -> impl Future<Output = reqwest::Result<QueryResponse>> {
        let mut url = self.base_uri.0.join("query").unwrap();
        url.query_pairs_mut().append_pair("target", query_name.0.as_str());
        let request = self.client.get(url);
        self.do_request(
            request,
            |response| Box::pin(response.json::<QueryResponse>())
        )
    }

    pub fn notify(
        &self, data: &NotifyRequest
    ) -> impl Future<Output = reqwest::Result<()>> {
        let url = self.base_uri.0.join("notify").unwrap();
        let request = self.client.post(url).json(data);
        self.do_request(
            request,
            |response| Box::pin(future::ready(
                response.error_for_status().map(|_| ())
            ))
        )
    }

    pub fn resolve(
        &self, data: &ResolveRequest
    ) -> impl Future<Output = reqwest::Result<()>> {
        let url = self.base_uri.0.join("resolve").unwrap();
        let request = self.client.post(url).json(data);
        self.do_request(
            request,
            |response| Box::pin(future::ready(
                response.error_for_status().map(|_| ())
            ))
        )
    }

    fn do_request<Parsed : Debug>(
        &self, request: RequestBuilder,
        parse: impl FnOnce(Response) -> Pin<Box<dyn Future<Output = reqwest::Result<Parsed>> + Send>>
    ) -> impl Future<Output = reqwest::Result<Parsed>> {
        let semaphore = self.max_concurrent_requests.clone();
        // Untie lifetime of the returned future from `self`.
        async move {
            debug!("Getting a permit...");
            let _permit = semaphore.acquire().await.unwrap();
            debug!("Permit acquired, sending request.");
            let response = request.send().await?;
            let parsed = parse(response).await;
            debug!("Response received: {:?}", parsed);
            parsed
        }
    }
}