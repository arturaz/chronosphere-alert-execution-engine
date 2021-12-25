use std::future::Future;
use reqwest::Url;
use crate::data::httpapi::{NotifyRequest, QueryAlertsResponse, QueryResponse, ResolveRequest};
use crate::data::httpapi::alerts::QueryName;

#[derive(Clone)]
pub struct BaseUri(pub Url);

#[derive(Clone)]
pub struct Client {
    base_uri: BaseUri,
    client: reqwest::Client
}

impl Client {
    pub fn new(base_uri: BaseUri) -> Client {
        Client { base_uri, client: reqwest::Client::new() }
    }

    pub fn query_alerts(&self) -> impl Future<Output = reqwest::Result<QueryAlertsResponse>> {
        let url = self.base_uri.0.join("alerts").unwrap();
        let request = self.client.get(url).send();
        // Untie lifetime of the returned future from self.
        async move {
            request.await?.json::<QueryAlertsResponse>().await
        }
    }

    pub fn query(
        &self, query_name: &QueryName
    ) -> impl Future<Output = reqwest::Result<QueryResponse>> {
        let mut url = self.base_uri.0.join("query").unwrap();
        url.query_pairs_mut().append_pair("target", query_name.0.as_str());
        let request = self.client.get(url).send();
        // Untie lifetime of the returned future from self.
        async move {
            request.await?.json::<QueryResponse>().await
        }
    }

    pub fn notify(
        &self, data: &NotifyRequest
    ) -> impl Future<Output = reqwest::Result<()>> {
        let url = self.base_uri.0.join("notify").unwrap();
        let request = self.client.post(url).json(data).send();
        // Untie lifetime of the returned future from self.
        async move {
            let response = request.await?;
            response.error_for_status().map(|_| ())
        }
    }

    pub fn resolve(
        &self, data: &ResolveRequest
    ) -> impl Future<Output = reqwest::Result<()>> {
        let url = self.base_uri.0.join("resolve").unwrap();
        let request = self.client.post(url).json(data).send();
        // Untie lifetime of the returned future from self.
        async move {
            let response = request.await?;
            response.error_for_status().map(|_| ())
        }
    }
}