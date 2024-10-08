use std::error::Error;
use structopt::StructOpt;
use reqwest::Url;
use tower::{ServiceBuilder};
use crate::alerts_api::MaxConcurrentRequests;
use crate::api::alerts_api;
use crate::api::alerts_api::BaseUri;
use crate::utils::InfiniteRetries;

pub mod api;
pub mod data;
pub mod engine;
pub mod utils;

#[derive(Debug, StructOpt)]
#[structopt(name = "alert-execution-engine")]
struct CliArgs {
    /// Url where the alerts validator is running.
    #[structopt(short, long, default_value="http://localhost:9001")]
    url: Url,
    /// Allows you to limit maximum number of requests to the URL made concurrently.
    #[structopt(short, long)]
    max_concurrent_requests: Option<usize>
}

#[tokio::main]
async fn main() -> Result<(), Box<dyn Error>> {
    log4rs::init_file("log4rs.yml", Default::default()).unwrap();

    let args: CliArgs = CliArgs::from_args();

    let client = alerts_api::Client::new(
        BaseUri(args.url),
        args.max_concurrent_requests.map(|v| MaxConcurrentRequests(v))
    );

    let policy: InfiniteRetries = Default::default();
    let query_alerts = {
        // Clone the client so it could be moved to the closure ownership.
        let client = client.clone();
        ServiceBuilder::new().retry(policy.clone())
            .service_fn(move |_: ()| client.query_alerts())
    };
    let do_query = {
        let client = client.clone();
        ServiceBuilder::new().retry(policy.clone())
            .service_fn(move |req| client.query(&req))
    };
    let do_notify = {
        let client = client.clone();
        ServiceBuilder::new().retry(policy.clone())
            .service_fn(move |req| client.notify(&req))
    };
    let do_resolve = {
        let client = client.clone();
        ServiceBuilder::new().retry(policy.clone())
            .service_fn(move |req| client.resolve(&req))
    };
    let _ = engine::initialize(query_alerts, do_query, do_notify, do_resolve).await?;

    Ok(())
}