#![allow(dead_code)] // tests/ の各ファイルが別クレートとして common を取り込むため、使わない関数の警告を抑える

use app::{build_app, config::Config, state::AppState};
use axum::{Router, body::Body, http::Request, http::StatusCode};
use http_body_util::BodyExt;
use sqlx::SqlitePool;
use std::sync::Arc;
use tower::ServiceExt;

pub const TEST_API_KEY: &str = "test-api-key";

pub fn test_app(pool: SqlitePool) -> Router {
    let config = Config {
        host: "127.0.0.1".to_string(),
        port: 0,
        database_url: "unused".to_string(),
        api_key: TEST_API_KEY.to_string(),
    };
    build_app(AppState {
        pool,
        config: Arc::new(config),
    })
}

pub async fn send(app: Router, request: Request<Body>) -> (StatusCode, serde_json::Value) {
    let response = app.oneshot(request).await.unwrap();
    let status = response.status();
    let bytes = response.into_body().collect().await.unwrap().to_bytes();
    let json = serde_json::from_slice(&bytes).unwrap();
    (status, json)
}

pub fn get(uri: &str) -> Request<Body> {
    Request::get(uri).body(Body::empty()).unwrap()
}

pub fn json_request(
    method: &str,
    uri: &str,
    api_key: Option<&str>,
    body: &'static str,
) -> Request<Body> {
    let mut builder = Request::builder()
        .method(method)
        .uri(uri)
        .header("content-type", "application/json");
    if let Some(key) = api_key {
        builder = builder.header("x-api-key", key);
    }
    builder.body(Body::from(body)).unwrap()
}
