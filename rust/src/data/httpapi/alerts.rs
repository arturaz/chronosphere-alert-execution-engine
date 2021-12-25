use std::time::Duration;
use serde::{Serialize, Deserialize};

#[derive(Serialize, Deserialize, Debug, Clone)]
pub struct AlertName(pub String);

#[derive(Serialize, Deserialize, Debug, Clone)]
pub struct QueryName(pub String);

#[derive(Serialize, Deserialize, Debug, Clone, Eq, PartialEq)]
pub struct AlertMessage(pub String);

#[derive(Serialize, Deserialize, Debug, PartialOrd, PartialEq)]
pub struct AlertThresholdValue(pub f64);

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
impl AlertThresholds {
    pub fn state_for(&self, value: &AlertThresholdValue) -> AlertState {
        if value >= &self.critical.value {
            AlertState::Critical { message: &self.critical.message }
        }
        else if value >= &self.warn.value {
            AlertState::Warn { message: &self.warn.message }
        }
        else {
            AlertState::Pass
        }
    }
}

#[derive(Eq, PartialEq, Debug)]
pub enum AlertState<'message> {
    /// [AlertThresholdValue] < [AlertThresholds.warn]
    Pass,
    /// [AlertThresholdValue] >= [AlertThresholds.warn]
    Warn { message: &'message AlertMessage },
    /// [AlertThresholdValue] >= [AlertThresholds.critical]
    Critical { message: &'message AlertMessage },
}