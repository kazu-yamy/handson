mod common;

use app::token::hash_token;
use axum::http::StatusCode;
use common::{
    TEST_SESSION_TTL_SECS, authed_json, get, json_request, register_user, send, test_app,
};
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
async fn login_with_a_too_long_password_is_unauthorized(pool: SqlitePool) {
    // 128 文字を超えて、本文の上限（64 KiB）には収まる長さ。もっと大きい本文は 413 になる（tests/errors.rs）
    let (status, body) = send(
        test_app(pool),
        login_request("alice@example.com", &"x".repeat(1_000)),
    )
    .await;
    assert_eq!(status, StatusCode::UNAUTHORIZED);
    assert_eq!(body["error"]["message"], "invalid email or password");
}

#[sqlx::test]
async fn me_returns_the_owner_of_the_token(pool: SqlitePool) {
    let app = test_app(pool);
    let alice = register_user(&app, "alice").await;

    let (status, body) = send(app, authed_json("GET", "/auth/me", &alice.token, "")).await;
    assert_eq!(status, StatusCode::OK);
    assert_eq!(
        body,
        serde_json::json!({ "id": alice.id, "name": "alice", "email": "alice@example.com" })
    );
}

#[sqlx::test]
async fn me_without_a_valid_token_is_unauthorized(pool: SqlitePool) {
    use axum::{body::Body, http::Request};
    use tower::ServiceExt;
    let app = test_app(pool);
    let alice = register_user(&app, "alice").await;

    let with_header = |value: Option<String>| {
        let mut builder = Request::get("/auth/me");
        if let Some(value) = value {
            builder = builder.header("authorization", value);
        }
        builder.body(Body::empty()).unwrap()
    };
    let cases = [
        None,                                        // ヘッダー無し
        Some("Bearer".to_string()),                  // トークン無し
        Some("Bearer ".to_string()),                 // トークンが空
        Some("Bearer    ".to_string()),              // 空白だけ
        Some(format!("Basic {}", alice.token)),      // スキームが違う
        Some(alice.token.clone()),                   // スキームが無い
        Some("Bearer not-a-real-token".to_string()), // 存在しないトークン
    ];
    for value in cases {
        let response = app
            .clone()
            .oneshot(with_header(value.clone()))
            .await
            .unwrap();
        assert_eq!(response.status(), StatusCode::UNAUTHORIZED, "{value:?}");
        assert_eq!(
            response.headers()["www-authenticate"],
            "Bearer",
            "{value:?}"
        );
    }

    // スキーム名の大文字小文字は区別しない
    let response = app
        .oneshot(with_header(Some(format!("bearer {}", alice.token))))
        .await
        .unwrap();
    assert_eq!(response.status(), StatusCode::OK);
}

// RFC 9110 では、スキームとトークンの間は空白 1 個以上（1*SP）
#[sqlx::test]
async fn several_spaces_after_bearer_are_accepted(pool: SqlitePool) {
    use axum::{body::Body, http::Request};
    use tower::ServiceExt;
    let app = test_app(pool);
    let alice = register_user(&app, "alice").await;

    let request = Request::get("/auth/me")
        .header("authorization", format!("Bearer   {}", alice.token))
        .body(Body::empty())
        .unwrap();
    let response = app.oneshot(request).await.unwrap();
    assert_eq!(response.status(), StatusCode::OK);
}

#[sqlx::test]
async fn logout_invalidates_only_that_token(pool: SqlitePool) {
    let app = test_app(pool.clone());
    let alice = register_user(&app, "alice").await;
    // 同じ利用者が別の端末でもログインしている状態
    let (_, second) = send(
        app.clone(),
        login_request("alice@example.com", "password-123456"),
    )
    .await;
    let second_token = second["token"].as_str().unwrap().to_string();

    let (status, body) = send(
        app.clone(),
        authed_json("POST", "/auth/logout", &alice.token, ""),
    )
    .await;
    assert_eq!(status, StatusCode::NO_CONTENT);
    assert_eq!(body, serde_json::Value::Null);

    // ログアウトしたトークンは 401。sessions の行も消えている
    let (status, _) = send(
        app.clone(),
        authed_json("GET", "/auth/me", &alice.token, ""),
    )
    .await;
    assert_eq!(status, StatusCode::UNAUTHORIZED);
    let remaining: i64 = sqlx::query_scalar("SELECT COUNT(*) FROM sessions")
        .fetch_one(&pool)
        .await
        .unwrap();
    assert_eq!(remaining, 1);

    // 別のトークンはそのまま使える
    let (status, _) = send(
        app.clone(),
        authed_json("GET", "/auth/me", &second_token, ""),
    )
    .await;
    assert_eq!(status, StatusCode::OK);

    // ログアウト済みのトークンでもう一度ログアウトすると 401（2 回目は認証で弾かれる）
    let (status, _) = send(app, authed_json("POST", "/auth/logout", &alice.token, "")).await;
    assert_eq!(status, StatusCode::UNAUTHORIZED);
}

#[sqlx::test]
async fn logout_without_a_token_is_unauthorized(pool: SqlitePool) {
    let (status, body) = send(
        test_app(pool),
        json_request("POST", "/auth/logout", None, ""),
    )
    .await;
    assert_eq!(status, StatusCode::UNAUTHORIZED);
    assert_eq!(body["error"]["code"], "unauthorized");
}

#[sqlx::test]
async fn an_expired_token_is_unauthorized(pool: SqlitePool) {
    let app = test_app(pool.clone());
    let alice = register_user(&app, "alice").await;

    // 期限を 1 秒前に書き換える（DB の時計で判定するので、過去の UNIX 秒にすれば期限切れ）
    sqlx::query("UPDATE sessions SET expires_at = unixepoch() - 1")
        .execute(&pool)
        .await
        .unwrap();

    let (status, body) = send(app, authed_json("GET", "/auth/me", &alice.token, "")).await;
    assert_eq!(status, StatusCode::UNAUTHORIZED);
    assert_eq!(body["error"]["code"], "unauthorized");
}

#[sqlx::test]
async fn login_deletes_expired_sessions(pool: SqlitePool) {
    let app = test_app(pool.clone());
    register_user(&app, "alice").await;
    register_user(&app, "bob").await;
    // alice のセッションだけを期限切れにする
    sqlx::query(
        "UPDATE sessions SET expires_at = unixepoch() - 1 \
         WHERE user_id = (SELECT user_id FROM credentials WHERE email = 'alice@example.com')",
    )
    .execute(&pool)
    .await
    .unwrap();

    // bob のログインで、alice の期限切れセッションが掃除される（bob の 2 件は残る）
    send(app, login_request("bob@example.com", "password-123456")).await;
    let remaining: Vec<String> = sqlx::query_scalar(
        "SELECT c.email FROM sessions s JOIN credentials c ON c.user_id = s.user_id ORDER BY s.created_at, c.email",
    )
    .fetch_all(&pool)
    .await
    .unwrap();
    assert_eq!(remaining, vec!["bob@example.com", "bob@example.com"]);
}
