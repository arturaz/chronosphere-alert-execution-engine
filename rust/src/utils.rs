use std::cmp::min;
use std::future::Future;
use std::pin::Pin;
use std::sync::Arc;
use std::time::Duration;
use log::info;
use tower::retry::Policy;

pub mod seconds_duration_format {
    use std::time::Duration;
    use serde::{Deserialize, Deserializer, Serializer};

    pub fn serialize<S>(duration: &Duration, serializer: S) -> Result<S::Ok, S::Error>
        where S: Serializer
    {
        serializer.serialize_f64(duration.as_secs_f64())
    }

    pub fn deserialize<'de, D>(deserializer: D) -> Result<Duration, D::Error>
        where D: Deserializer<'de>,
    {
        Ok(Duration::from_secs_f64(Deserialize::deserialize(deserializer)?))
    }
}

/// Specifies a schedule for retrying things.
#[derive(Clone)]
pub struct BackoffSchedule {
    schedule: Arc<Vec<Duration>>,
    current_idx: usize
}
impl BackoffSchedule {
    /// Returns the current [Duration] and moves the [current_idx] by one.
    pub fn advance(&mut self) -> Duration {
        let duration = self.schedule[self.current_idx];
        self.current_idx = min(self.schedule.len() - 1, self.current_idx + 1);
        duration
    }
}
impl Default for BackoffSchedule {
    fn default() -> Self {
        BackoffSchedule {
            schedule: Arc::new(vec![
                Duration::from_millis(100), Duration::from_millis(250),
                Duration::from_millis(500), Duration::from_secs(1),
                Duration::from_secs(2)
            ]),
            current_idx: 0
        }
    }
}

/// Retry [Policy] that retries forever according to the [BackoffSchedule].
#[derive(Clone, Default)]
pub struct InfiniteRetries(BackoffSchedule);

impl<Request : Clone, Response, Error> Policy<Request, Response, Error> for InfiniteRetries {
    type Future = Pin<Box<dyn Future<Output = Self> + Send>>;

    fn retry(&self, _req: &Request, result: Result<&Response, &Error>) -> Option<Self::Future> {
        match result {
            Ok(_) => None,
            Err(_) => {
                let mut schedule = self.0.clone();
                let sleep_for = schedule.advance();
                Some(Box::pin(async move {
                    info!("Sleeping for {:?} before next retry", sleep_for);
                    tokio::time::sleep(sleep_for).await;
                    InfiniteRetries(schedule)
                }))
            }
        }
    }

    fn clone_request(&self, req: &Request) -> Option<Request> {
        Some(req.clone())
    }
}