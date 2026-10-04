mod common;

use axum::http::StatusCode;
use common::{get, send, test_app};
use sqlx::SqlitePool;

#[sqlx::test]
async fn health_returns_ok(pool: SqlitePool) {
    let (status, body) = send(test_app(pool), get("/health")).await;
    assert_eq!(status, StatusCode::OK);
    assert_eq!(body, serde_json::json!({ "status": "ok" }));
}
