use std::cmp::min;
use std::time::Duration;
use futures_util::future;
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

#[derive(Clone)]
pub struct BackoffSchedule {
    schedule: Vec<Duration>,
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

#[derive(Clone)]
pub struct InfiniteRetries/*(BackoffSchedule)*/;

impl<Request : Clone, Response, Error> Policy<Request, Response, Error> for InfiniteRetries {
    type Future = future::Ready<Self>;

    fn retry(&self, _req: &Request, result: Result<&Response, &Error>) -> Option<Self::Future> {
        match result {
            Ok(_) => None,
            Err(_) => Some(future::ready(InfiniteRetries))
        }
    }

    fn clone_request(&self, req: &Request) -> Option<Request> {
        Some(req.clone())
    }
}