mod common;

use axum::{
    body::Body,
    http::{HeaderValue, Request, StatusCode},
};
use common::{get, json_request, send_raw, test_app, test_app_with_config, test_config};
use sqlx::SqlitePool;

#[sqlx::test]
async fn every_response_gets_a_uuid_request_id(pool: SqlitePool) {
    let (status, headers, _) = send_raw(test_app(pool), get("/health")).await;

    assert_eq!(status, StatusCode::OK);
    let id = headers["x-request-id"].to_str().unwrap();
    // UUID v4 の文字列は 36 文字（8-4-4-4-12）
    assert_eq!(id.len(), 36);
    assert_eq!(id.matches('-').count(), 4);
}

#[sqlx::test]
async fn each_request_gets_a_different_id(pool: SqlitePool) {
    let app = test_app(pool);
    let (_, first, _) = send_raw(app.clone(), get("/health")).await;
    let (_, second, _) = send_raw(app, get("/health")).await;

    assert_ne!(first["x-request-id"], second["x-request-id"]);
}

#[sqlx::test]
async fn a_request_id_sent_by_the_client_is_kept(pool: SqlitePool) {
    let request = Request::get("/health")
        .header("x-request-id", "from-the-client-123")
        .body(Body::empty())
        .unwrap();
    let (_, headers, _) = send_raw(test_app(pool), request).await;

    assert_eq!(headers["x-request-id"], "from-the-client-123");
}

#[sqlx::test]
async fn error_responses_from_the_router_and_extractors_also_have_a_request_id(pool: SqlitePool) {
    let app = test_app(pool);

    // 404（未定義のルート）、405（定義の無いメソッド）、422（検証エラー）
    let (status, headers, _) = send_raw(app.clone(), get("/nope")).await;
    assert_eq!(status, StatusCode::NOT_FOUND);
    assert!(headers.contains_key("x-request-id"));

    let delete = Request::delete("/health").body(Body::empty()).unwrap();
    let (status, headers, _) = send_raw(app.clone(), delete).await;
    assert_eq!(status, StatusCode::METHOD_NOT_ALLOWED);
    assert!(headers.contains_key("x-request-id"));

    let invalid = json_request("POST", "/auth/register", None, r#"{"name":""}"#);
    let (status, headers, _) = send_raw(app, invalid).await;
    assert_eq!(status, StatusCode::UNPROCESSABLE_ENTITY);
    assert!(headers.contains_key("x-request-id"));
}

#[sqlx::test]
async fn the_cors_preflight_response_also_has_a_request_id(pool: SqlitePool) {
    let mut config = test_config();
    config.cors_allowed_origins = vec![HeaderValue::from_static("http://localhost:3000")];
    let preflight = Request::builder()
        .method("OPTIONS")
        .uri("/posts")
        .header("origin", "http://localhost:3000")
        .header("access-control-request-method", "POST")
        .body(Body::empty())
        .unwrap();
    let (status, headers, _) = send_raw(test_app_with_config(pool, config), preflight).await;

    // 内側の層を呼ばずに CORS が返す応答にも付く（PropagateRequestIdLayer が CORS の外側にあるため）
    assert_eq!(status, StatusCode::OK);
    assert!(headers.contains_key("x-request-id"));
}
