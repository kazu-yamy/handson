#![allow(dead_code)] // tests/ の各ファイルが別クレートとして common を取り込むため、使わない関数の警告を抑える

use app::{build_app, config::Config, rate_limit::RateLimits, state::AppState};
use axum::{
    Router,
    body::Body,
    extract::connect_info::MockConnectInfo,
    http::{HeaderMap, Request, StatusCode},
};
use http_body_util::BodyExt;
use sqlx::SqlitePool;
use std::net::SocketAddr;
use std::sync::Arc;
use std::time::Duration;
use tower::ServiceExt;

pub const TEST_SESSION_TTL_SECS: i64 = 3600;

pub fn test_config() -> Config {
    Config {
        host: "127.0.0.1".to_string(),
        port: 0,
        database_url: "unused".to_string(),
        session_ttl_secs: TEST_SESSION_TTL_SECS,
        request_timeout: Duration::from_secs(10),
        trust_forwarded_for: false,
        cors_allowed_origins: Vec::new(),
    }
}

// テストごとに新しい RateLimits を持つ（limiter は static ではなく AppState の中にある）ので、
// 並列に走るほかのテストの回数と混ざらない
pub fn test_state(pool: SqlitePool) -> AppState {
    AppState {
        pool,
        config: Arc::new(test_config()),
        limits: Arc::new(RateLimits::new()),
    }
}

// oneshot で呼ぶテストには、serve が付ける接続元のアドレス（ConnectInfo）が無い。
// MockConnectInfo で代わりの接続元を付ける。main.rs の into_make_service_with_connect_info の配線は、このテストでは守れない
pub fn app_from(state: AppState, ip: [u8; 4]) -> Router {
    build_app(state).layer(MockConnectInfo(SocketAddr::from((ip, 40000))))
}

pub fn app_with(state: AppState) -> Router {
    app_from(state, [127, 0, 0, 1])
}

// 設定（CORS の許可リストなど）を差し替えたいテスト用
pub fn test_app_with_config(pool: SqlitePool, config: Config) -> Router {
    let mut state = test_state(pool);
    state.config = Arc::new(config);
    app_with(state)
}

// 上限を差し替えたいテスト用（上限を超える件数を一度に送るテストなど）
pub fn test_app_with_limits(pool: SqlitePool, limits: RateLimits) -> Router {
    let mut state = test_state(pool);
    state.limits = Arc::new(limits);
    app_with(state)
}

pub fn test_app(pool: SqlitePool) -> Router {
    app_with(test_state(pool))
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

// ヘッダーと本文の文字列も見たいときに使う（JSON として読めない応答も確かめられる）
pub async fn send_raw(app: Router, request: Request<Body>) -> (StatusCode, HeaderMap, String) {
    let response = app.oneshot(request).await.unwrap();
    let status = response.status();
    let headers = response.headers().clone();
    let bytes = response.into_body().collect().await.unwrap().to_bytes();
    (status, headers, String::from_utf8(bytes.to_vec()).unwrap())
}

pub fn get(uri: &str) -> Request<Body> {
    Request::get(uri).body(Body::empty()).unwrap()
}

// token があれば Authorization: Bearer を付ける
pub fn json_request(method: &str, uri: &str, token: Option<&str>, body: &str) -> Request<Body> {
    let mut builder = Request::builder()
        .method(method)
        .uri(uri)
        .header("content-type", "application/json");
    if let Some(token) = token {
        builder = builder.header("authorization", format!("Bearer {token}"));
    }
    builder.body(Body::from(body.to_string())).unwrap()
}

// 記事を API 経由で 1 件作って、応答の JSON を返す（著者は token の持ち主）
pub async fn create_post(app: &Router, token: &str, title: &str) -> serde_json::Value {
    let (status, post) = send(
        app.clone(),
        authed_json(
            "POST",
            "/posts",
            token,
            &serde_json::json!({ "title": title, "body": "body" }).to_string(),
        ),
    )
    .await;
    assert_eq!(status, StatusCode::CREATED);
    post
}

// Bearer トークン付きのリクエスト。本文が要らないときは body に "" を渡す
pub fn authed_json(method: &str, uri: &str, token: &str, body: &str) -> Request<Body> {
    Request::builder()
        .method(method)
        .uri(uri)
        .header("content-type", "application/json")
        .header("authorization", format!("Bearer {token}"))
        .body(Body::from(body.to_string()))
        .unwrap()
}

// /auth/register で利用者を 1 人作る
pub struct TestUser {
    pub id: i64,
    pub token: String,
}

pub async fn register_user(app: &Router, name: &str) -> TestUser {
    let (status, body) = send(
        app.clone(),
        json_request(
            "POST",
            "/auth/register",
            None,
            &serde_json::json!({
                "name": name,
                "email": format!("{name}@example.com"),
                "password": "password-123456",
            })
            .to_string(),
        ),
    )
    .await;
    assert_eq!(status, StatusCode::CREATED);
    TestUser {
        id: body["user"]["id"].as_i64().unwrap(),
        token: body["token"].as_str().unwrap().to_string(),
    }
}
