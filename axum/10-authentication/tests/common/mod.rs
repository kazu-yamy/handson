#![allow(dead_code)] // tests/ の各ファイルが別クレートとして common を取り込むため、使わない関数の警告を抑える

use app::{build_app, config::Config, state::AppState};
use axum::{Router, body::Body, http::Request, http::StatusCode};
use http_body_util::BodyExt;
use sqlx::SqlitePool;
use std::sync::Arc;
use tower::ServiceExt;

pub const TEST_API_KEY: &str = "test-api-key";
pub const TEST_SESSION_TTL_SECS: i64 = 3600;

pub fn test_app(pool: SqlitePool) -> Router {
    let config = Config {
        host: "127.0.0.1".to_string(),
        port: 0,
        database_url: "unused".to_string(),
        api_key: TEST_API_KEY.to_string(),
        session_ttl_secs: TEST_SESSION_TTL_SECS,
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
    // 204 No Content のように本文が空の応答は Null として返す
    let json = if bytes.is_empty() {
        serde_json::Value::Null
    } else {
        serde_json::from_slice(&bytes).unwrap()
    };
    (status, json)
}

pub fn get(uri: &str) -> Request<Body> {
    Request::get(uri).body(Body::empty()).unwrap()
}

pub fn json_request(method: &str, uri: &str, api_key: Option<&str>, body: &str) -> Request<Body> {
    let mut builder = Request::builder()
        .method(method)
        .uri(uri)
        .header("content-type", "application/json");
    if let Some(key) = api_key {
        builder = builder.header("x-api-key", key);
    }
    builder.body(Body::from(body.to_string())).unwrap()
}

// 記事のテスト用に、API 経由で user を 1 人作って id を返す
pub async fn create_user(app: &Router, name: &str) -> i64 {
    let (status, user) = send(
        app.clone(),
        json_request(
            "POST",
            "/users",
            Some(TEST_API_KEY),
            &serde_json::json!({ "name": name }).to_string(),
        ),
    )
    .await;
    assert_eq!(status, StatusCode::CREATED);
    user["id"].as_i64().unwrap()
}

// 記事を API 経由で 1 件作って、応答の JSON を返す
pub async fn create_post(app: &Router, author_id: i64, title: &str) -> serde_json::Value {
    let (status, post) = send(
        app.clone(),
        json_request(
            "POST",
            "/posts",
            Some(TEST_API_KEY),
            &serde_json::json!({ "author_id": author_id, "title": title, "body": "body" })
                .to_string(),
        ),
    )
    .await;
    assert_eq!(status, StatusCode::CREATED);
    post
}
