use axum::{
    extract::{Request, State},
    http::StatusCode,
    middleware::Next,
    response::Response,
};
use std::time::Instant;

use crate::error::error_response;
use crate::state::AppState;

pub async fn log_request(req: Request, next: Next) -> Response {
    let method = req.method().clone();
    let uri = req.uri().clone();
    let start = Instant::now();

    let response = next.run(req).await;

    let elapsed = start.elapsed();
    println!("{method} {uri} -> {} ({elapsed:?})", response.status());
    response
}

pub async fn require_api_key(State(state): State<AppState>, req: Request, next: Next) -> Response {
    let is_valid = req
        .headers()
        .get("x-api-key")
        .and_then(|value| value.to_str().ok())
        == Some(state.config.api_key.as_str());

    if !is_valid {
        return error_response(
            StatusCode::UNAUTHORIZED,
            "unauthorized",
            "invalid or missing x-api-key",
        );
    }

    next.run(req).await
}
