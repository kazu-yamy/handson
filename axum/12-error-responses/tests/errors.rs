mod common;

use app::layers::{self, MAX_BODY_BYTES};
use axum::{Router, http::StatusCode, routing::get as get_route};
use common::{authed_json, get, json_request, register_user, send_raw, test_app, test_config};
use sqlx::SqlitePool;
use std::time::Duration;

// 応答が契約の JSON（content-type: application/json、{"error": {"code", "message"}}）であることを確かめる
fn assert_error(
    (status, headers, body): (StatusCode, axum::http::HeaderMap, String),
    expected_status: StatusCode,
    code: &str,
    message: &str,
) {
    assert_eq!(status, expected_status, "{body}");
    assert_eq!(headers["content-type"], "application/json", "{body}");
    let json: serde_json::Value = serde_json::from_str(&body).unwrap();
    assert_eq!(json["error"]["code"], code, "{body}");
    assert_eq!(json["error"]["message"], message, "{body}");
    assert!(json["error"].get("fields").is_none(), "{body}");
}

#[sqlx::test]
async fn a_path_that_is_not_an_integer_is_a_json_bad_request(pool: SqlitePool) {
    let app = test_app(pool);
    for uri in ["/posts/abc", "/users/abc", "/posts/abc/like"] {
        let method = if uri.ends_with("/like") {
            "POST"
        } else {
            "GET"
        };
        let response = send_raw(app.clone(), json_request(method, uri, None, "")).await;
        assert_error(
            response,
            StatusCode::BAD_REQUEST,
            "bad_request",
            "invalid path parameter",
        );
    }
}

#[sqlx::test]
async fn the_rejected_path_value_is_not_echoed(pool: SqlitePool) {
    let (_, _, body) = send_raw(test_app(pool), get("/posts/secret-value")).await;
    assert!(!body.contains("secret-value"), "{body}");
}

#[sqlx::test]
async fn a_malformed_query_is_a_json_bad_request(pool: SqlitePool) {
    let response = send_raw(test_app(pool), get("/posts?limit=abc")).await;
    assert_error(
        response,
        StatusCode::BAD_REQUEST,
        "bad_request",
        "invalid query string",
    );
}

#[sqlx::test]
async fn broken_json_is_a_bad_request(pool: SqlitePool) {
    let request = json_request("POST", "/auth/login", None, r#"{"email":"a@example.com","#);
    let response = send_raw(test_app(pool), request).await;
    assert_error(
        response,
        StatusCode::BAD_REQUEST,
        "bad_request",
        "request body is not valid JSON",
    );
}

#[sqlx::test]
async fn a_json_type_mismatch_does_not_echo_the_value(pool: SqlitePool) {
    let request = json_request(
        "POST",
        "/auth/login",
        None,
        r#"{"email":"a@example.com","password":12345}"#,
    );
    let response = send_raw(test_app(pool), request).await;
    assert!(!response.2.contains("12345"), "{}", response.2);
    assert_error(
        response,
        StatusCode::UNPROCESSABLE_ENTITY,
        "validation_failed",
        "request body does not match the expected format",
    );
}

#[sqlx::test]
async fn a_missing_field_is_unprocessable_without_fields(pool: SqlitePool) {
    let request = json_request("POST", "/auth/login", None, r#"{"email":"a@example.com"}"#);
    let response = send_raw(test_app(pool), request).await;
    assert_error(
        response,
        StatusCode::UNPROCESSABLE_ENTITY,
        "validation_failed",
        "request body does not match the expected format",
    );
}

#[sqlx::test]
async fn a_body_without_a_json_content_type_is_unsupported(pool: SqlitePool) {
    let request = axum::http::Request::post("/auth/login")
        .body(axum::body::Body::from(r#"{"email":"a@example.com"}"#))
        .unwrap();
    let response = send_raw(test_app(pool), request).await;
    assert_error(
        response,
        StatusCode::UNSUPPORTED_MEDIA_TYPE,
        "unsupported_media_type",
        "content-type must be application/json",
    );
}

#[sqlx::test]
async fn a_missing_method_is_a_json_405_with_an_allow_header(pool: SqlitePool) {
    let app = test_app(pool);
    let (status, headers, body) =
        send_raw(app.clone(), json_request("POST", "/users", None, "{}")).await;
    assert_eq!(headers["allow"], "GET,HEAD");
    assert_error(
        (status, headers, body),
        StatusCode::METHOD_NOT_ALLOWED,
        "method_not_allowed",
        "method not allowed",
    );

    // 複数のメソッドがあるルートでは Allow に全部並ぶ
    let (status, headers, body) = send_raw(app, json_request("PUT", "/posts/1", None, "{}")).await;
    assert_eq!(headers["allow"], "GET,HEAD,PATCH,DELETE");
    assert_error(
        (status, headers, body),
        StatusCode::METHOD_NOT_ALLOWED,
        "method_not_allowed",
        "method not allowed",
    );
}

#[sqlx::test]
async fn an_unknown_route_is_a_json_404(pool: SqlitePool) {
    let response = send_raw(test_app(pool), get("/no/such/route")).await;
    assert_error(
        response,
        StatusCode::NOT_FOUND,
        "not_found",
        "route not found",
    );
}

// `{"title":"t","body":"b"}` に空白を足して、本文をちょうど len バイトにする
fn post_body_of_len(len: usize) -> String {
    let json = r#"{"title":"t","body":"b"}"#;
    format!("{json}{}", " ".repeat(len - json.len()))
}

#[sqlx::test]
async fn a_body_over_the_limit_is_413_and_exactly_the_limit_is_accepted(pool: SqlitePool) {
    let app = test_app(pool);
    let alice = register_user(&app, "alice").await;

    let body = post_body_of_len(MAX_BODY_BYTES);
    let (status, _, text) = send_raw(
        app.clone(),
        authed_json("POST", "/posts", &alice.token, &body),
    )
    .await;
    assert_eq!(status, StatusCode::CREATED, "{text}");

    let body = post_body_of_len(MAX_BODY_BYTES + 1);
    let response = send_raw(app, authed_json("POST", "/posts", &alice.token, &body)).await;
    assert_error(
        response,
        StatusCode::PAYLOAD_TOO_LARGE,
        "payload_too_large",
        "request body is too large",
    );
}

#[sqlx::test]
async fn a_70_kb_title_is_413_not_a_validation_error(pool: SqlitePool) {
    let app = test_app(pool);
    let alice = register_user(&app, "alice").await;
    let body = serde_json::json!({ "title": "a".repeat(70_000), "body": "b" }).to_string();
    let response = send_raw(app, authed_json("POST", "/posts", &alice.token, &body)).await;
    assert_error(
        response,
        StatusCode::PAYLOAD_TOO_LARGE,
        "payload_too_large",
        "request body is too large",
    );
}

// タイムアウトの層はデータベースを使わないので、遅いハンドラ 1 つだけの Router に layers::apply を掛けて確かめる
#[tokio::test]
async fn a_slow_handler_is_a_json_408() {
    let mut config = test_config();
    config.request_timeout = Duration::from_millis(50);
    let router = Router::new().route(
        "/slow",
        get_route(|| async {
            tokio::time::sleep(Duration::from_secs(5)).await;
            "too late"
        }),
    );
    let app = layers::apply(router, &config);

    let response = send_raw(app, get("/slow")).await;
    assert_error(
        response,
        StatusCode::REQUEST_TIMEOUT,
        "timeout",
        "request timed out",
    );
}

// 本文の無いエラーを返す層やハンドラがあっても、安全網が契約の JSON を補い、Allow などのヘッダーは残す
#[tokio::test]
async fn an_error_without_a_body_is_filled_in_and_keeps_its_headers() {
    let router = Router::new()
        .route(
            "/bare",
            get_route(|| async { StatusCode::SERVICE_UNAVAILABLE }),
        )
        .route(
            "/allow",
            get_route(|| async { (StatusCode::METHOD_NOT_ALLOWED, [("allow", "GET")]) }),
        );
    let app = layers::apply(router, &test_config());

    let response = send_raw(app.clone(), get("/bare")).await;
    assert_error(
        response,
        StatusCode::SERVICE_UNAVAILABLE,
        "unavailable",
        "internal server error",
    );

    let (status, headers, body) = send_raw(app, get("/allow")).await;
    assert_eq!(headers["allow"], "GET");
    assert_error(
        (status, headers, body),
        StatusCode::METHOD_NOT_ALLOWED,
        "method_not_allowed",
        "method not allowed",
    );
}

// 11 では 100 KB のパスワードも email も 401 まで進んでいた（持ち越し 4）。12 からは本文の上限で手前の 413 になる
#[sqlx::test]
async fn a_huge_login_email_or_password_is_413(pool: SqlitePool) {
    let app = test_app(pool);
    for body in [
        serde_json::json!({ "email": format!("{}@example.com", "a".repeat(70_000)), "password": "x" }),
        serde_json::json!({ "email": "alice@example.com", "password": "x".repeat(100_000) }),
    ] {
        let response = send_raw(
            app.clone(),
            json_request("POST", "/auth/login", None, &body.to_string()),
        )
        .await;
        assert_error(
            response,
            StatusCode::PAYLOAD_TOO_LARGE,
            "payload_too_large",
            "request body is too large",
        );
    }
}
