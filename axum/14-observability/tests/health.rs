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

#[sqlx::test]
async fn ready_returns_ok_while_the_database_answers(pool: SqlitePool) {
    let (status, body) = send(test_app(pool), get("/ready")).await;
    assert_eq!(status, StatusCode::OK);
    assert_eq!(body, serde_json::json!({ "status": "ok" }));
}

#[sqlx::test]
async fn ready_returns_503_once_the_pool_is_closed(pool: SqlitePool) {
    let app = test_app(pool.clone());
    // プールを閉じると、DB に届かない状態になる（clone も同じプールを指すので全体が閉じる）
    pool.close().await;

    let (status, body) = send(app.clone(), get("/ready")).await;
    assert_eq!(status, StatusCode::SERVICE_UNAVAILABLE);
    assert_eq!(
        body,
        serde_json::json!({ "error": { "code": "unavailable", "message": "service unavailable" } })
    );

    // /health はプロセスが生きているかだけを見るので、DB が落ちていても 200 のまま
    let (status, _) = send(app, get("/health")).await;
    assert_eq!(status, StatusCode::OK);
}
