use std::fmt::Debug;
use std::future::Future;
use std::pin::Pin;
use futures_util::future::{Abortable, AbortHandle};
use log::{debug, info};
use tokio::time::sleep;
use tower::Service;
use crate::data::httpapi::alerts::{Alert, AlertMessage, AlertName, AlertState, QueryName};
use crate::data::httpapi::{AlertStatusChangeRequest, NotifyRequest, QueryAlertsResponse, QueryResponse, ResolveRequest};

const LOG_TARGET: &str = "alerts_engine";
fn log_target_alert(alert: &AlertName) -> String {
    format!("{}::{}", LOG_TARGET, alert.0)
}

pub async fn initialize<
    Err : 'static + Send + Debug,
    DoQuery : 'static + Service<QueryName, Error = Err, Response = QueryResponse> + Clone + Send,
    Notify : 'static + Service<NotifyRequest, Error = Err, Response = ()> + Clone + Send,
    Resolve : 'static + Service<ResolveRequest, Error = Err, Response = ()> + Clone + Send,
>(
    mut query_alerts: impl Service<(), Error = Err, Response = QueryAlertsResponse>,
    query: DoQuery, notify: Notify, resolve: Resolve
) -> Result<(), Err>
    where
        <DoQuery as Service<QueryName>>::Future : Send,
        <Notify as Service<NotifyRequest>>::Future : Send,
        <Resolve as Service<ResolveRequest>>::Future : Send,
{
    info!(target: LOG_TARGET, "Querying alerts...");
    let alerts: QueryAlertsResponse = query_alerts.call(()).await?;
    info!(target: LOG_TARGET, "Alerts queried:\n{:?}", alerts);

    let launched_alerts =
        alerts.alerts.into_iter()
            .map(|alert| (
                alert.name.clone(),
                tokio::spawn(initialize_alert(
                    alert, query.clone(), notify.clone(), resolve.clone()
                ))
            ))
            .collect::<Vec<_>>();

    for (alert_name, join_handle) in launched_alerts {
        let res = join_handle.await;
        info!(target: log_target_alert(&alert_name).as_str(), "Alert engine finished with {:?}", res);
    }

    Ok(())
}

/// Watches a single [Alert].
pub async fn initialize_alert<Err : Send + 'static, DoNotify, DoResolve>(
    alert: Alert,
    mut query: impl Service<QueryName, Error = Err, Response = QueryResponse>,
    notify: DoNotify,
    resolve: DoResolve,
) -> Result<(), Err>
where
    DoNotify : Service<NotifyRequest, Error = Err, Response = ()> + Clone + Send + 'static,
    <DoNotify as Service<NotifyRequest>>::Future : Send,
    DoResolve : Service<ResolveRequest, Error = Err, Response = ()> + Clone + Send + 'static,
    <DoResolve as Service<ResolveRequest>>::Future : Send
{
    let log_target = log_target_alert(&alert.name);
    let create_notify_request = |message: AlertMessage| {
        AlertStatusChangeRequest::Notify {
            request: NotifyRequest { alert_name: alert.name.clone(), message },
            repeat_after: alert.repeat_interval.clone()
        }
    };

    info!(target: &log_target, "Starting engine.");
    let mut state = AlertState::Pass;
    let mut maybe_reporter: Option<AbortHandle> = None;
    loop {
        debug!(target: &log_target, "Querying {:?}", alert.name);
        let response: QueryResponse = query.call(alert.query.clone()).await?;
        debug!(target: &log_target, "Received response: {:?}", response.value);
        let new_state = alert.thresholds.state_for(&response.value.into());
        if state == new_state {
            debug!(target: &log_target, "State did not change");
        }
        else {
            info!(target: &log_target, "State changed from {:?} to {:?}", state, new_state);
            state = new_state;

            if let Some(reporter) = &maybe_reporter {
                debug!(target: &log_target, "Detected previous reporter, stopping it.");
                reporter.abort();
            }
            maybe_reporter = Some(initialize_alert_reporter(
               alert.name.clone(),
                match state {
                    AlertState::Pass => AlertStatusChangeRequest::Resolve(ResolveRequest {
                        alert_name: alert.name.clone()
                    }),
                    AlertState::Warn { message } =>
                        create_notify_request(message.clone()),
                    AlertState::Critical { message } =>
                        create_notify_request(message.clone())
                },
                notify.clone(), resolve.clone()
            ));
        }
        debug!(target: &log_target, "Sleeping for {:?}", alert.interval);
        sleep(alert.interval).await;
    }
}

/// Initializes the reporter task, which runs in the background and can be stopped using the
/// returned [AbortHandle].
pub fn initialize_alert_reporter<Err : Send + 'static, DoNotify, DoResolve>(
    alert_name: AlertName,
    request: AlertStatusChangeRequest,
    mut notify: DoNotify,
    mut resolve: DoResolve,
) -> AbortHandle
    where
        DoNotify : Service<NotifyRequest, Error = Err, Response = ()> + Send + 'static,
        <DoNotify as Service<NotifyRequest>>::Future : Send,
        DoResolve : Service<ResolveRequest, Error = Err, Response = ()> + Send + 'static,
        <DoResolve as Service<ResolveRequest>>::Future : Send
{
    let mut log_target = log_target_alert(&alert_name);
    log_target.push_str("::reporter");
    let task: Pin<Box<dyn Future<Output = Result<(), Err>> + Send>> = match request {
        AlertStatusChangeRequest::Resolve(resolve_request) => {
            // Invoke the resolve once.
            let future = resolve.call(resolve_request);
            // Pin the future for the `spawn`.
            Box::pin(future)
        }
        AlertStatusChangeRequest::Notify { request, repeat_after } => {
            // Invoke the notify, then repeat every interval.
            let future = async move {
                loop {
                    debug!(target: &log_target, "Sending notify: {:?}", request);
                    notify.call(request.clone()).await?;
                    debug!(target: &log_target, "Sleeping next notify for {:?}", repeat_after);
                    sleep(repeat_after).await;
                }
            };
            // Pin the future for the `spawn`.
            Box::pin(future)
        }
    };
    let (abort_handle, abort_registration) = AbortHandle::new_pair();
    tokio::spawn( Abortable::new(task, abort_registration));
    // Return the abort handle so that we could stop the spawned task.
    abort_handle
}
