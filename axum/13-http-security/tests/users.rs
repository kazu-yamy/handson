mod common;

use axum::http::{Request, StatusCode};
use common::{get, json_request, register_user, send, test_app};
use sqlx::SqlitePool;
use tower::ServiceExt;

#[sqlx::test]
async fn registered_user_is_listed_and_fetchable_without_email(pool: SqlitePool) {
    let app = test_app(pool);
    let alice = register_user(&app, "alice").await;

    let (status, list) = send(app.clone(), get("/users")).await;
    assert_eq!(status, StatusCode::OK);
    assert_eq!(
        list,
        serde_json::json!([{ "id": alice.id, "name": "alice" }])
    );

    let (status, fetched) = send(app, get(&format!("/users/{}", alice.id))).await;
    assert_eq!(status, StatusCode::OK);
    assert_eq!(
        fetched,
        serde_json::json!({ "id": alice.id, "name": "alice" })
    );
}

#[sqlx::test]
async fn list_starts_empty(pool: SqlitePool) {
    let (status, body) = send(test_app(pool), get("/users")).await;
    assert_eq!(status, StatusCode::OK);
    assert_eq!(body, serde_json::json!([]));
}

#[sqlx::test]
async fn get_unknown_user_is_not_found(pool: SqlitePool) {
    let (status, body) = send(test_app(pool), get("/users/999")).await;
    assert_eq!(status, StatusCode::NOT_FOUND);
    assert_eq!(body["error"]["code"], "not_found");
}

#[sqlx::test]
async fn user_write_routes_are_gone(pool: SqlitePool) {
    let app = test_app(pool);
    let alice = register_user(&app, "alice").await;

    // 登録は /auth/register に一本化した。/users には読み取りのルートしか無いので 405
    let requests = [
        json_request("POST", "/users", Some(&alice.token), r#"{"name":"x"}"#),
        json_request("PUT", "/users/1", Some(&alice.token), r#"{"name":"x"}"#),
        json_request("DELETE", "/users/1", Some(&alice.token), ""),
    ];
    for request in requests {
        let label = format!("{} {}", request.method(), request.uri());
        let response = app.clone().oneshot(request).await.unwrap();
        assert_eq!(response.status(), StatusCode::METHOD_NOT_ALLOWED, "{label}");
    }

    // alice はそのまま残っている
    let (_, list) = send(app, get("/users")).await;
    assert_eq!(list.as_array().unwrap().len(), 1);
}

#[sqlx::test]
async fn the_old_api_key_header_no_longer_authenticates(pool: SqlitePool) {
    let app = test_app(pool);
    let request = Request::post("/posts")
        .header("content-type", "application/json")
        .header("x-api-key", "change-me-dev-only")
        .body(axum::body::Body::from(r#"{"title":"t","body":"b"}"#))
        .unwrap();
    let (status, body) = send(app, request).await;
    assert_eq!(status, StatusCode::UNAUTHORIZED);
    assert_eq!(body["error"]["code"], "unauthorized");
}
