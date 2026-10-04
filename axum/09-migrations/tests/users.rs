mod common;

use axum::http::StatusCode;
use common::{TEST_API_KEY, get, json_request, send, test_app};
use sqlx::SqlitePool;

#[sqlx::test]
async fn create_then_get_user(pool: SqlitePool) {
    let app = test_app(pool);

    let (status, created) = send(
        app.clone(),
        json_request("POST", "/users", Some(TEST_API_KEY), r#"{"name":"alice"}"#),
    )
    .await;
    assert_eq!(status, StatusCode::CREATED);
    assert_eq!(created["name"], "alice");
    let id = created["id"].as_i64().unwrap();

    let (status, fetched) = send(app, get(&format!("/users/{id}"))).await;
    assert_eq!(status, StatusCode::OK);
    assert_eq!(fetched, serde_json::json!({ "id": id, "name": "alice" }));
}

#[sqlx::test]
async fn list_starts_empty(pool: SqlitePool) {
    let (status, body) = send(test_app(pool), get("/users")).await;
    assert_eq!(status, StatusCode::OK);
    assert_eq!(body, serde_json::json!([]));
}

#[sqlx::test]
async fn create_without_api_key_is_unauthorized(pool: SqlitePool) {
    let (status, body) = send(
        test_app(pool),
        json_request("POST", "/users", None, r#"{"name":"alice"}"#),
    )
    .await;
    assert_eq!(status, StatusCode::UNAUTHORIZED);
    assert_eq!(body["error"]["code"], "unauthorized");
}

#[sqlx::test]
async fn create_with_empty_name_is_unprocessable(pool: SqlitePool) {
    let (status, body) = send(
        test_app(pool),
        json_request("POST", "/users", Some(TEST_API_KEY), r#"{"name":"   "}"#),
    )
    .await;
    assert_eq!(status, StatusCode::UNPROCESSABLE_ENTITY);
    assert_eq!(body["error"]["code"], "validation_failed");
    assert!(body["error"]["fields"]["name"].is_array());
}

#[sqlx::test]
async fn get_unknown_user_is_not_found(pool: SqlitePool) {
    let (status, body) = send(test_app(pool), get("/users/999")).await;
    assert_eq!(status, StatusCode::NOT_FOUND);
    assert_eq!(body["error"]["code"], "not_found");
}

#[sqlx::test]
async fn create_with_broken_json_is_bad_request(pool: SqlitePool) {
    let (status, body) = send(
        test_app(pool),
        json_request("POST", "/users", Some(TEST_API_KEY), r#"{"name":"#),
    )
    .await;
    assert_eq!(status, StatusCode::BAD_REQUEST);
    assert_eq!(body["error"]["code"], "bad_request");
}
