mod common;

use app::layers;
use axum::{Router, http::HeaderMap, routing::get as get_route};
use axum::{
    body::Body,
    http::{HeaderValue, Request, StatusCode},
};
use common::{get, json_request, send_raw, test_app, test_app_with_config, test_config};
use sqlx::SqlitePool;
use std::time::Duration;

const ALLOWED: &str = "http://localhost:3000";

fn cors_app(pool: SqlitePool) -> axum::Router {
    let mut config = test_config();
    config.cors_allowed_origins = vec![HeaderValue::from_static(ALLOWED)];
    test_app_with_config(pool, config)
}

fn preflight(origin: &str) -> Request<Body> {
    Request::builder()
        .method("OPTIONS")
        .uri("/posts")
        .header("origin", origin)
        .header("access-control-request-method", "POST")
        .header(
            "access-control-request-headers",
            "authorization,content-type",
        )
        .body(Body::empty())
        .unwrap()
}

#[sqlx::test]
async fn preflight_from_an_allowed_origin_is_answered(pool: SqlitePool) {
    let (status, headers, _) = send_raw(cors_app(pool), preflight(ALLOWED)).await;

    assert_eq!(status, StatusCode::OK);
    assert_eq!(headers["access-control-allow-origin"], ALLOWED);
    assert_eq!(
        headers["access-control-allow-methods"],
        "GET,POST,PATCH,DELETE"
    );
    assert_eq!(
        headers["access-control-allow-headers"],
        "authorization,content-type"
    );
    assert_eq!(headers["access-control-max-age"], "600");
    // credentials は許可しない
    assert!(!headers.contains_key("access-control-allow-credentials"));
}

#[sqlx::test]
async fn preflight_from_an_unlisted_origin_gets_no_allow_origin(pool: SqlitePool) {
    // 末尾に / を付けた書き方も「完全一致しない別のオリジン」として扱われる
    for origin in [
        "http://evil.example",
        "http://localhost:3000/",
        "http://localhost:3001",
    ] {
        let (_, headers, _) = send_raw(cors_app(pool.clone()), preflight(origin)).await;
        assert!(
            !headers.contains_key("access-control-allow-origin"),
            "{origin} must not be allowed"
        );
    }
}

#[sqlx::test]
async fn cors_is_off_by_default_so_preflight_is_method_not_allowed(pool: SqlitePool) {
    let (status, headers, body) = send_raw(test_app(pool), preflight(ALLOWED)).await;

    assert_eq!(status, StatusCode::METHOD_NOT_ALLOWED);
    assert!(!headers.contains_key("access-control-allow-origin"));
    assert_eq!(
        body,
        r#"{"error":{"code":"method_not_allowed","message":"method not allowed"}}"#
    );
}

#[sqlx::test]
async fn cors_headers_are_also_on_error_responses_including_429(pool: SqlitePool) {
    let app = cors_app(pool);
    let login = |origin: &'static str| {
        let mut request = json_request(
            "POST",
            "/auth/login",
            None,
            r#"{"email":"alice@example.com","password":"wrong-password-123"}"#,
        );
        request
            .headers_mut()
            .insert("origin", HeaderValue::from_static(origin));
        request
    };

    for _ in 0..5 {
        send_raw(app.clone(), login(ALLOWED)).await;
    }
    let (status, headers, _) = send_raw(app.clone(), login(ALLOWED)).await;
    assert_eq!(status, StatusCode::TOO_MANY_REQUESTS);
    assert_eq!(headers["access-control-allow-origin"], ALLOWED);
    // ブラウザの JavaScript が Retry-After を読めるように expose している
    assert_eq!(headers["access-control-expose-headers"], "retry-after");
    assert!(headers.contains_key("retry-after"));

    // 404 にも付く。許可していないオリジンには付かない
    let mut request = get("/nope");
    request
        .headers_mut()
        .insert("origin", HeaderValue::from_static(ALLOWED));
    let (status, headers, _) = send_raw(app.clone(), request).await;
    assert_eq!(status, StatusCode::NOT_FOUND);
    assert_eq!(headers["access-control-allow-origin"], ALLOWED);
    let mut request = get("/nope");
    request
        .headers_mut()
        .insert("origin", HeaderValue::from_static("http://evil.example"));
    let (_, headers, _) = send_raw(app, request).await;
    assert!(!headers.contains_key("access-control-allow-origin"));
}

const SECURITY_HEADERS: [(&str, &str); 4] = [
    ("x-content-type-options", "nosniff"),
    (
        "content-security-policy",
        "default-src 'none'; frame-ancestors 'none'",
    ),
    ("referrer-policy", "no-referrer"),
    ("cache-control", "no-store"),
];

fn assert_security_headers(headers: &HeaderMap, what: &str) {
    for (name, value) in SECURITY_HEADERS {
        assert_eq!(
            headers.get(name).and_then(|v| v.to_str().ok()),
            Some(value),
            "{what}: {name}"
        );
    }
}

#[sqlx::test]
async fn security_headers_are_on_success_and_error_responses(pool: SqlitePool) {
    let app = test_app(pool);
    let oversized = "x".repeat(70 * 1024);
    let cases: Vec<(&str, StatusCode, Request<Body>)> = vec![
        ("200 health", StatusCode::OK, get("/health")),
        ("400 bad path", StatusCode::BAD_REQUEST, get("/posts/abc")),
        ("404 no route", StatusCode::NOT_FOUND, get("/nope")),
        (
            "405 no method",
            StatusCode::METHOD_NOT_ALLOWED,
            Request::delete("/health").body(Body::empty()).unwrap(),
        ),
        (
            "401 login",
            StatusCode::UNAUTHORIZED,
            json_request(
                "POST",
                "/auth/login",
                None,
                r#"{"email":"a@example.com","password":"wrong-password-123"}"#,
            ),
        ),
        (
            "413 too large",
            StatusCode::PAYLOAD_TOO_LARGE,
            json_request(
                "POST",
                "/auth/login",
                None,
                &format!(r#"{{"email":"{oversized}","password":"x"}}"#),
            ),
        ),
    ];
    for (what, expected, request) in cases {
        let (status, headers, _) = send_raw(app.clone(), request).await;
        assert_eq!(status, expected, "{what}");
        assert_security_headers(&headers, what);
    }
}

#[sqlx::test]
async fn security_headers_are_on_429_and_on_a_filled_in_408(pool: SqlitePool) {
    // 429（レート制限）
    let app = test_app(pool);
    let login = || {
        json_request(
            "POST",
            "/auth/login",
            None,
            r#"{"email":"a@example.com","password":"wrong-password-123"}"#,
        )
    };
    for _ in 0..5 {
        send_raw(app.clone(), login()).await;
    }
    let (status, headers, _) = send_raw(app, login()).await;
    assert_eq!(status, StatusCode::TOO_MANY_REQUESTS);
    assert_security_headers(&headers, "429");

    // 408（TimeoutLayer が返した本文の無い応答を、安全網が JSON に補ったもの）
    let mut config = test_config();
    config.request_timeout = Duration::from_millis(50);
    let slow = Router::new().route(
        "/slow",
        get_route(|| async {
            tokio::time::sleep(Duration::from_millis(500)).await;
            "late"
        }),
    );
    let (status, headers, _) = send_raw(layers::apply(slow, &config), get("/slow")).await;
    assert_eq!(status, StatusCode::REQUEST_TIMEOUT);
    assert_security_headers(&headers, "408");
}

#[tokio::test]
async fn a_header_set_by_the_handler_is_not_overwritten() {
    let router = Router::new().route(
        "/cached",
        get_route(|| async { ([("cache-control", "max-age=60")], "ok") }),
    );
    let (_, headers, _) = send_raw(layers::apply(router, &test_config()), get("/cached")).await;

    assert_eq!(headers["cache-control"], "max-age=60"); // if_not_present なので、ハンドラの値が残る
    assert_eq!(headers["x-content-type-options"], "nosniff"); // 付いていないものだけが補われる
}
