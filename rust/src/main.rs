use std::error::Error;
use reqwest::Url;
use tower::{ServiceBuilder};
use crate::api::alerts_api;
use crate::api::alerts_api::BaseUri;
use crate::utils::InfiniteRetries;

pub mod api;
pub mod data;
pub mod engine;
pub mod utils;

#[tokio::main]
async fn main() -> Result<(), Box<dyn Error>> {
    log4rs::init_file("log4rs.yml", Default::default()).unwrap();

    let base_uri = BaseUri(Url::parse("http://localhost:9001").unwrap());
    let client = alerts_api::Client::new(base_uri);

    let _ = engine::initialize(
        ServiceBuilder::new().retry(InfiniteRetries)
            .service_fn(|_: ()| client.query_alerts())
    ).await?;
    //
    // let res = client.query(&alerts.alerts[0].query).await?;
    // println!("query: {:?}", res);
    //
    // let res = client.notify(&NotifyRequest {
    //     alert_name: &alerts.alerts[0].name,
    //     message: &alerts.alerts[0].thresholds.critical.message
    // }).await?;
    // println!("notify: {:?}", res);

    Ok(())
}