use std::time::Duration;
use crate::data::httpapi::alerts::{Alert, AlertMessage, AlertName, AlertThresholdValue};
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
impl From<QueryValue> for AlertThresholdValue {
    fn from(v: QueryValue) -> Self { AlertThresholdValue(v.0) }
}

#[derive(Deserialize, Debug)]
pub struct QueryResponse {
    pub value: QueryValue
}

/// Request body for `/notify` endpoint.
#[derive(Serialize, Debug, Clone)]
pub struct NotifyRequest {
    #[serde(rename = "alertName")]
    pub alert_name: AlertName,
    pub message: AlertMessage
}

/// Request body for `/resolve` endpoint.
#[derive(Serialize, Debug, Clone)]
pub struct ResolveRequest {
    #[serde(rename = "alertName")]
    pub alert_name: AlertName
}

pub enum AlertStatusChangeRequest {
    Notify { request: NotifyRequest, repeat_after: Duration },
    Resolve(ResolveRequest)
}