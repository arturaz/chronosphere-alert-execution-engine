use std::time::Duration;
use serde::{Serialize, Deserialize};

#[derive(Serialize, Deserialize, Debug)]
pub struct AlertName(String);

#[derive(Serialize, Deserialize, Debug)]
pub struct QueryName(String);

#[derive(Serialize, Deserialize, Debug)]
pub struct AlertMessage(String);

#[derive(Serialize, Deserialize, Debug)]
pub struct AlertThresholdValue(f64);

#[derive(Serialize, Deserialize, Debug)]
pub struct Alert {
    pub name: AlertName,
    pub query: QueryName,
    #[serde(rename = "intervalSecs", with = "crate::utils::seconds_duration_format")]
    pub interval: Duration,
    #[serde(rename = "repeatIntervalSecs", with = "crate::utils::seconds_duration_format")]
    pub repeat_interval: Duration,
    #[serde(flatten)]
    pub thresholds: AlertThresholds
}

#[derive(Serialize, Deserialize, Debug)]
pub struct AlertThreshold {
    pub message: AlertMessage,
    pub value: AlertThresholdValue,
}

#[derive(Serialize, Deserialize, Debug)]
pub struct AlertThresholds {
    pub warn: AlertThreshold,
    pub critical: AlertThreshold,
}