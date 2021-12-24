use crate::data::httpapi::alerts::{Alert, AlertMessage, AlertName};
use serde::{Serialize, Deserialize};

pub mod alerts;

#[derive(Deserialize, Debug)]
#[serde(transparent)]
pub struct QueryAlertsResponse {
    pub alerts: Vec<Alert>
}

#[derive(Deserialize, Debug)]
#[serde(transparent)]
pub struct QueryValue(pub f64);

#[derive(Deserialize, Debug)]
pub struct QueryResponse {
    pub value: QueryValue
}

/// Request body for `/notify` endpoint.
#[derive(Serialize, Debug)]
pub struct NotifyRequest<'l> {
    #[serde(rename = "alertName")]
    pub alert_name: &'l AlertName,
    pub message: &'l AlertMessage
}

/// Request body for `/resolve` endpoint.
#[derive(Serialize, Debug)]
pub struct ResolveRequest<'l> {
    #[serde(rename = "alertName")]
    pub alert_name: &'l AlertName
}