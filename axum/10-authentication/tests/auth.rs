mod common;

use app::token::hash_token;
use axum::http::StatusCode;
use common::{TEST_SESSION_TTL_SECS, get, json_request, send, test_app};
use sqlx::SqlitePool;

fn register_body(name: &str, email: &str, password: &str) -> String {
    serde_json::json!({ "name": name, "email": email, "password": password }).to_string()
}

fn register_request(
    name: &str,
    email: &str,
    password: &str,
) -> axum::http::Request<axum::body::Body> {
    json_request(
        "POST",
        "/auth/register",
        None,
        &register_body(name, email, password),
    )
}

#[sqlx::test]
async fn register_creates_user_and_stores_only_the_hash(pool: SqlitePool) {
    let app = test_app(pool.clone());

    let (status, body) = send(
        app.clone(),
        register_request("alice", "alice@example.com", "password-123456"),
    )
    .await;
    assert_eq!(status, StatusCode::CREATED);
    assert_eq!(body["user"]["name"], "alice");
    // 応答のキーは user・token・expires_at だけ。パスワードもハッシュもメールアドレスも含めない
    let mut keys: Vec<&String> = body.as_object().unwrap().keys().collect();
    keys.sort();
    assert_eq!(keys, ["expires_at", "token", "user"]);
    assert_eq!(body["user"].as_object().unwrap().len(), 2);
    assert!(!body.to_string().contains("password"));

    // DB には平文ではなく argon2id のハッシュ（PHC 文字列）が入っている
    let stored: String = sqlx::query_scalar(
        "SELECT password_hash FROM credentials WHERE email = 'alice@example.com'",
    )
    .fetch_one(&pool)
    .await
    .unwrap();
    assert!(stored.starts_with("$argon2id$"));
    assert!(!stored.contains("password-123456"));

    // 登録した user は既存の GET /users からも見える
    let (_, users) = send(app, get("/users")).await;
    assert_eq!(
        users,
        serde_json::json!([{ "id": body["user"]["id"], "name": "alice" }])
    );
}

#[sqlx::test]
async fn register_trims_and_lowercases_the_email(pool: SqlitePool) {
    let (status, _) = send(
        test_app(pool.clone()),
        register_request("alice", "  Alice@Example.COM ", "password-123456"),
    )
    .await;
    assert_eq!(status, StatusCode::CREATED);

    let email: String = sqlx::query_scalar("SELECT email FROM credentials")
        .fetch_one(&pool)
        .await
        .unwrap();
    assert_eq!(email, "alice@example.com");
}

#[sqlx::test]
async fn register_with_duplicate_email_is_conflict_and_rolls_back(pool: SqlitePool) {
    let app = test_app(pool.clone());
    let (status, _) = send(
        app.clone(),
        register_request("alice", "alice@example.com", "password-123456"),
    )
    .await;
    assert_eq!(status, StatusCode::CREATED);

    // 大文字小文字だけが違うメールも重複として扱う
    let (status, body) = send(
        app,
        register_request("mallory", "ALICE@example.com", "another-password"),
    )
    .await;
    assert_eq!(status, StatusCode::CONFLICT);
    assert_eq!(body["error"]["code"], "conflict");

    // 2 回目の users への INSERT はロールバックされ、mallory の行は残らない
    let names: Vec<String> = sqlx::query_scalar("SELECT name FROM users ORDER BY id")
        .fetch_all(&pool)
        .await
        .unwrap();
    assert_eq!(names, vec!["alice".to_string()]);
}

#[sqlx::test]
async fn register_reports_every_invalid_field_at_once(pool: SqlitePool) {
    let (status, body) = send(
        test_app(pool.clone()),
        register_request("   ", "no-at-sign", "short"),
    )
    .await;
    assert_eq!(status, StatusCode::UNPROCESSABLE_ENTITY);
    assert_eq!(body["error"]["code"], "validation_failed");
    for field in ["name", "email", "password"] {
        assert!(body["error"]["fields"][field].is_array(), "{field}");
    }
    assert_eq!(
        body["error"]["fields"]["password"][0],
        "password must be 15 to 128 characters"
    );

    // 検証で弾かれたときは何も保存されない
    let count: i64 = sqlx::query_scalar("SELECT COUNT(*) FROM users")
        .fetch_one(&pool)
        .await
        .unwrap();
    assert_eq!(count, 0);
}

#[sqlx::test]
async fn register_rejects_a_password_over_128_characters(pool: SqlitePool) {
    let (status, body) = send(
        test_app(pool),
        register_request("alice", "alice@example.com", &"x".repeat(129)),
    )
    .await;
    assert_eq!(status, StatusCode::UNPROCESSABLE_ENTITY);
    assert!(body["error"]["fields"]["password"].is_array());
}

fn login_request(email: &str, password: &str) -> axum::http::Request<axum::body::Body> {
    json_request(
        "POST",
        "/auth/login",
        None,
        &serde_json::json!({ "email": email, "password": password }).to_string(),
    )
}

fn now_unix() -> i64 {
    std::time::SystemTime::now()
        .duration_since(std::time::UNIX_EPOCH)
        .unwrap()
        .as_secs() as i64
}

#[sqlx::test]
async fn register_returns_a_token_and_stores_only_its_hash(pool: SqlitePool) {
    let (status, body) = send(
        test_app(pool.clone()),
        register_request("alice", "alice@example.com", "password-123456"),
    )
    .await;
    assert_eq!(status, StatusCode::CREATED);

    // トークンは 32 バイトの乱数を base64url（パディング無し）にした 43 文字
    let token = body["token"].as_str().unwrap();
    assert_eq!(token.len(), 43);
    // 期限は「今 + TTL」あたり
    let expires_at = body["expires_at"].as_i64().unwrap();
    assert!((expires_at - (now_unix() + TEST_SESSION_TTL_SECS)).abs() <= 5);

    // sessions には SHA-256（32 バイト）だけがあり、平文のトークンは入っていない
    let stored: Vec<u8> = sqlx::query_scalar("SELECT token_hash FROM sessions")
        .fetch_one(&pool)
        .await
        .unwrap();
    assert_eq!(stored.len(), 32);
    assert_eq!(stored, hash_token(token));
    let plain_matches: i64 =
        sqlx::query_scalar("SELECT COUNT(*) FROM sessions WHERE token_hash = CAST(? AS BLOB)")
            .bind(token)
            .fetch_one(&pool)
            .await
            .unwrap();
    assert_eq!(plain_matches, 0);
}

#[sqlx::test]
async fn login_with_right_password_returns_a_new_token(pool: SqlitePool) {
    let app = test_app(pool.clone());
    let (_, registered) = send(
        app.clone(),
        register_request("alice", "alice@example.com", "password-123456"),
    )
    .await;

    // メールの大文字小文字や前後の空白は無視する
    let (status, body) = send(app, login_request(" Alice@Example.com ", "password-123456")).await;
    assert_eq!(status, StatusCode::OK);
    assert_eq!(body["user"], registered["user"]);
    assert_ne!(body["token"], registered["token"]); // ログインのたびに新しいトークン
    assert!(body["expires_at"].as_i64().unwrap() > now_unix());

    let sessions: i64 = sqlx::query_scalar("SELECT COUNT(*) FROM sessions")
        .fetch_one(&pool)
        .await
        .unwrap();
    assert_eq!(sessions, 2); // 登録時とログイン時
}

#[sqlx::test]
async fn login_failures_look_the_same(pool: SqlitePool) {
    let app = test_app(pool.clone());
    send(
        app.clone(),
        register_request("alice", "alice@example.com", "password-123456"),
    )
    .await;

    let (wrong_status, wrong_password) = send(
        app.clone(),
        login_request("alice@example.com", "wrong-password"),
    )
    .await;
    let (unknown_status, unknown_email) = send(
        app.clone(),
        login_request("nobody@example.com", "password-123456"),
    )
    .await;

    // パスワード違いも未登録のメールも、同じステータス・同じ本文
    assert_eq!(wrong_status, StatusCode::UNAUTHORIZED);
    assert_eq!(unknown_status, StatusCode::UNAUTHORIZED);
    assert_eq!(wrong_password, unknown_email);
    assert_eq!(wrong_password["error"]["code"], "unauthorized");
    assert_eq!(
        wrong_password["error"]["message"],
        "invalid email or password"
    );

    // 失敗したログインではセッションが増えない（登録時の 1 件のまま）
    let sessions: i64 = sqlx::query_scalar("SELECT COUNT(*) FROM sessions")
        .fetch_one(&pool)
        .await
        .unwrap();
    assert_eq!(sessions, 1);
}

#[sqlx::test]
async fn login_failure_has_www_authenticate_header(pool: SqlitePool) {
    use tower::ServiceExt;
    let response = test_app(pool)
        .oneshot(login_request("nobody@example.com", "password-123456"))
        .await
        .unwrap();
    assert_eq!(response.status(), StatusCode::UNAUTHORIZED);
    assert_eq!(response.headers()["www-authenticate"], "Bearer");
}

#[sqlx::test]
async fn login_with_a_huge_password_is_unauthorized(pool: SqlitePool) {
    let (status, body) = send(
        test_app(pool),
        login_request("alice@example.com", &"x".repeat(100_000)),
    )
    .await;
    assert_eq!(status, StatusCode::UNAUTHORIZED);
    assert_eq!(body["error"]["message"], "invalid email or password");
}
