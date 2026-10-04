use axum::{extract::Request, middleware::Next, response::Response};
use std::time::Instant;

pub async fn log_request(req: Request, next: Next) -> Response {
    let method = req.method().clone();
    let uri = req.uri().clone();
    let start = Instant::now();

    let response = next.run(req).await;

    let elapsed = start.elapsed();
    println!("{method} {uri} -> {} ({elapsed:?})", response.status());
    response
}
