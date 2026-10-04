mod common;

use app::rate_limit::RateLimits;
use axum::{
    Router,
    http::{HeaderMap, StatusCode},
};
use common::{
    app_from, app_with, authed_json, create_post, json_request, register_user, send_raw, test_app,
    test_config, test_state,
};
use governor::Quota;
use sqlx::SqlitePool;
use std::sync::Arc;
use std::time::Duration;

fn login(email: &str, password: &str) -> axum::http::Request<axum::body::Body> {
    json_request(
        "POST",
        "/auth/login",
        None,
        &serde_json::json!({ "email": email, "password": password }).to_string(),
    )
}

async fn login_status(
    app: &Router,
    email: &str,
    password: &str,
) -> (StatusCode, HeaderMap, String) {
    send_raw(app.clone(), login(email, password)).await
}

#[sqlx::test]
async fn sixth_login_for_the_same_email_is_rate_limited(pool: SqlitePool) {
    let app = test_app(pool);

    for attempt in 1..=5 {
        let (status, _, _) = login_status(&app, "alice@example.com", "wrong-password-123").await;
        assert_eq!(status, StatusCode::UNAUTHORIZED, "attempt {attempt}");
    }
    let (status, headers, body) =
        login_status(&app, "alice@example.com", "wrong-password-123").await;
    assert_eq!(status, StatusCode::TOO_MANY_REQUESTS);
    // 回復は 12 秒ごとに 1 回分。直前に使い切ったので待ち時間は 12 秒に近い（切り上げ、最小 1）
    let retry_after: u64 = headers["retry-after"].to_str().unwrap().parse().unwrap();
    assert!(
        (1..=12).contains(&retry_after),
        "retry-after: {retry_after}"
    );
    assert_eq!(
        body,
        r#"{"error":{"code":"rate_limited","message":"too many requests"}}"#
    );
}

#[sqlx::test]
async fn another_email_is_counted_separately(pool: SqlitePool) {
    let app = test_app(pool);

    for _ in 0..6 {
        login_status(&app, "alice@example.com", "wrong-password-123").await;
    }
    let (status, _, _) = login_status(&app, "bob@example.com", "wrong-password-123").await;
    assert_eq!(status, StatusCode::UNAUTHORIZED);
}

#[sqlx::test]
async fn registered_and_unregistered_emails_are_limited_the_same_way(pool: SqlitePool) {
    // 同じ limiter を共有しつつ、IP の上限（10 回）に当たらないよう、email ごとに別の接続元から送る
    let state = test_state(pool);
    register_user(&app_from(state.clone(), [192, 0, 2, 9]), "alice").await; // alice@example.com は登録済み

    for (email, ip) in [
        ("alice@example.com", [192, 0, 2, 1]),
        ("nobody@example.com", [192, 0, 2, 2]), // 未登録
    ] {
        let app = app_from(state.clone(), ip);
        let mut statuses = Vec::new();
        for _ in 0..7 {
            let (status, _, _) = login_status(&app, email, "wrong-password-123").await;
            statuses.push(status.as_u16());
        }
        // 登録済みかどうかで、429 になる回数も応答も変わらない（制限が登録の有無の手がかりにならない）
        assert_eq!(statuses, [401, 401, 401, 401, 401, 429, 429], "{email}");
    }
}

#[sqlx::test]
async fn correct_password_is_rejected_while_over_the_limit(pool: SqlitePool) {
    let app = test_app(pool);
    register_user(&app, "alice").await;

    for _ in 0..5 {
        login_status(&app, "alice@example.com", "wrong-password-123").await;
    }
    // 上限を超えている間は、正しいパスワードでも 429（パスワードの当たり外れを教えない）
    let (status, _, _) = login_status(&app, "alice@example.com", "password-123456").await;
    assert_eq!(status, StatusCode::TOO_MANY_REQUESTS);
}

#[sqlx::test]
async fn email_is_normalized_before_counting(pool: SqlitePool) {
    let app = test_app(pool);

    // 大文字小文字と前後の空白を変えても、同じ email として数える
    for email in [
        "alice@example.com",
        "ALICE@example.com",
        " alice@example.com",
        "Alice@Example.com",
        "alice@example.com ",
    ] {
        let (status, _, _) = login_status(&app, email, "wrong-password-123").await;
        assert_eq!(status, StatusCode::UNAUTHORIZED, "{email}");
    }
    let (status, _, _) = login_status(&app, "aLiCe@example.com", "wrong-password-123").await;
    assert_eq!(status, StatusCode::TOO_MANY_REQUESTS);
}

#[sqlx::test]
async fn email_over_254_characters_is_unauthorized_and_never_reaches_the_limiter(pool: SqlitePool) {
    let state = test_state(pool);
    let limits = state.limits.clone();
    let app = app_with(state);
    let long_email = format!("{}@example.com", "a".repeat(250));
    assert!(long_email.len() > 254);

    // 何回送っても 401（429 にならない）。同じ文言で、limiter にキーも作られない
    for _ in 0..10 {
        let (status, _, body) = login_status(&app, &long_email, "wrong-password-123").await;
        assert_eq!(status, StatusCode::UNAUTHORIZED);
        assert_eq!(
            body,
            r#"{"error":{"code":"unauthorized","message":"invalid email or password"}}"#
        );
    }
    assert_eq!(limits.login_by_email.len(), 0);
}

#[sqlx::test]
async fn the_limit_recovers_as_time_passes(pool: SqlitePool) {
    // 実時間で 12 秒待たなくてよいように、回復を 300 ミリ秒ごと・バースト 2 回にした limiter を差し込む
    let mut state = test_state(pool);
    let quick = Quota::with_period(Duration::from_millis(300))
        .unwrap()
        .allow_burst(std::num::NonZeroU32::new(2).unwrap());
    state.limits = Arc::new(RateLimits::with_quotas(
        quick,
        Quota::per_minute(std::num::NonZeroU32::new(100).unwrap()),
        Quota::per_minute(std::num::NonZeroU32::new(100).unwrap()),
    ));
    let app = app_with(state);

    // 失敗するログインは argon2 の検証を通るので 1 回に数十ミリ秒かかり、負荷が高いと 3 回目までに 1 回分
    // 回復してしまうことがある。「3 回目で必ず 429」と決め打ちせず、429 になるまで送る（上限 10 回）
    let mut limited = false;
    for _ in 0..10 {
        let (status, _, _) = login_status(&app, "alice@example.com", "wrong-password-123").await;
        if status == StatusCode::TOO_MANY_REQUESTS {
            limited = true;
            break;
        }
        assert_eq!(status, StatusCode::UNAUTHORIZED);
    }
    assert!(limited, "never rate limited");

    // 429 になった直後から 400 ミリ秒待てば（回復の間隔は 300 ミリ秒）、必ず 1 回分は回復している
    tokio::time::sleep(Duration::from_millis(400)).await;
    let (status, _, _) = login_status(&app, "alice@example.com", "wrong-password-123").await;
    assert_eq!(status, StatusCode::UNAUTHORIZED);
}

fn register(name: &str, email: &str) -> axum::http::Request<axum::body::Body> {
    json_request(
        "POST",
        "/auth/register",
        None,
        &serde_json::json!({ "name": name, "email": email, "password": "password-123456" })
            .to_string(),
    )
}

// email を毎回変えて email 単位の制限を避け、IP 単位の制限だけを確かめる
async fn login_as_stranger(app: &Router, n: usize) -> StatusCode {
    login_status(app, &format!("user{n}@example.com"), "wrong-password-123")
        .await
        .0
}

#[sqlx::test]
async fn eleventh_login_from_the_same_ip_is_rate_limited(pool: SqlitePool) {
    let app = test_app(pool);

    for n in 1..=10 {
        assert_eq!(
            login_as_stranger(&app, n).await,
            StatusCode::UNAUTHORIZED,
            "{n}"
        );
    }
    let (status, headers, _) = login_status(&app, "user11@example.com", "wrong-password-123").await;
    assert_eq!(status, StatusCode::TOO_MANY_REQUESTS);
    let retry_after: u64 = headers["retry-after"].to_str().unwrap().parse().unwrap();
    assert!((1..=6).contains(&retry_after), "retry-after: {retry_after}");
}

#[sqlx::test]
async fn register_and_login_share_one_ip_allowance(pool: SqlitePool) {
    let app = test_app(pool);

    for n in 1..=5 {
        let (status, _, _) = send_raw(
            app.clone(),
            register(&format!("u{n}"), &format!("u{n}@example.com")),
        )
        .await;
        assert_eq!(status, StatusCode::CREATED, "register {n}");
    }
    for n in 1..=5 {
        assert_eq!(
            login_as_stranger(&app, n).await,
            StatusCode::UNAUTHORIZED,
            "login {n}"
        );
    }
    // 登録とログインで合わせて 10 回使ったので、11 回目は登録でもログインでも 429
    let (status, _, _) = send_raw(app.clone(), register("u99", "u99@example.com")).await;
    assert_eq!(status, StatusCode::TOO_MANY_REQUESTS);
    assert_eq!(
        login_as_stranger(&app, 99).await,
        StatusCode::TOO_MANY_REQUESTS
    );
}

#[sqlx::test]
async fn another_ip_has_its_own_allowance(pool: SqlitePool) {
    // 同じ AppState（＝同じ limiter）を共有する 2 つの Router。接続元の IP だけが違う
    let state = test_state(pool);
    let from_a = app_from(state.clone(), [192, 0, 2, 1]);
    let from_b = app_from(state, [192, 0, 2, 2]);

    for n in 1..=10 {
        login_as_stranger(&from_a, n).await;
    }
    assert_eq!(
        login_as_stranger(&from_a, 11).await,
        StatusCode::TOO_MANY_REQUESTS
    );
    assert_eq!(
        login_as_stranger(&from_b, 11).await,
        StatusCode::UNAUTHORIZED
    );
}

#[sqlx::test]
async fn thirty_first_like_from_the_same_ip_is_rate_limited(pool: SqlitePool) {
    let app = test_app(pool);
    let user = register_user(&app, "alice").await;
    let post = create_post(&app, &user.token, "hello").await;
    let like_uri = format!("/posts/{}/like", post["id"]);

    for n in 1..=30 {
        let (status, _, _) =
            send_raw(app.clone(), authed_json("POST", &like_uri, &user.token, "")).await;
        assert_eq!(status, StatusCode::OK, "like {n}");
    }
    let (status, headers, _) =
        send_raw(app.clone(), authed_json("POST", &like_uri, &user.token, "")).await;
    assert_eq!(status, StatusCode::TOO_MANY_REQUESTS);
    assert!(headers.contains_key("retry-after"));

    // 制限が掛かるのは like だけ。同じ記事の読み取りや、ログインの枠は影響を受けない
    let (status, _, _) =
        send_raw(app.clone(), common::get(&format!("/posts/{}", post["id"]))).await;
    assert_eq!(status, StatusCode::OK);
    assert_eq!(login_as_stranger(&app, 1).await, StatusCode::UNAUTHORIZED);
}

#[sqlx::test]
async fn forwarded_for_cannot_be_used_to_escape_the_limit_by_default(pool: SqlitePool) {
    let app = test_app(pool); // trust_forwarded_for = false
    let with_header = |n: usize| {
        let mut request = login_request(&format!("user{n}@example.com"));
        request
            .headers_mut()
            .insert("x-forwarded-for", format!("203.0.113.{n}").parse().unwrap());
        request
    };

    // 毎回違う X-Forwarded-For を付けても、接続元の IP で数えるので 11 回目は 429
    for n in 1..=10 {
        let (status, _, _) = send_raw(app.clone(), with_header(n)).await;
        assert_eq!(status, StatusCode::UNAUTHORIZED, "{n}");
    }
    let (status, _, _) = send_raw(app.clone(), with_header(11)).await;
    assert_eq!(status, StatusCode::TOO_MANY_REQUESTS);
}

fn login_request(email: &str) -> axum::http::Request<axum::body::Body> {
    login(email, "wrong-password-123")
}

#[sqlx::test]
async fn trusted_forwarded_for_counts_the_rightmost_address(pool: SqlitePool) {
    let mut state = test_state(pool);
    let mut config = test_config();
    config.trust_forwarded_for = true;
    state.config = Arc::new(config);
    let app = app_with(state);
    let send_as = |n: usize, forwarded_for: &str| {
        let mut request = login_request(&format!("user{n}@example.com"));
        request
            .headers_mut()
            .insert("x-forwarded-for", forwarded_for.parse().unwrap());
        send_raw(app.clone(), request)
    };

    // 右端が同じ 203.0.113.1 なら、左端をどれだけ変えても同じ枠（左端を偽っても回避できない）
    for n in 1..=10 {
        let (status, _, _) = send_as(n, &format!("198.51.100.{n}, 203.0.113.1")).await;
        assert_eq!(status, StatusCode::UNAUTHORIZED, "{n}");
    }
    let (status, _, _) = send_as(11, "198.51.100.77, 203.0.113.1").await;
    assert_eq!(status, StatusCode::TOO_MANY_REQUESTS);
    // 右端が違えば別の枠
    let (status, _, _) = send_as(12, "198.51.100.77, 203.0.113.2").await;
    assert_eq!(status, StatusCode::UNAUTHORIZED);
}
