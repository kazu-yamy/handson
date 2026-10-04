use axum::{Json, extract::State, http::StatusCode};
use serde::{Deserialize, Serialize};
use sqlx::SqliteConnection;
use std::collections::BTreeMap;

use crate::auth_user::AuthUser;
use crate::error::AppError;
use crate::extract::AppJson;
use crate::password::{hash_password_blocking, verify_password_blocking};
use crate::rate_limit;
use crate::routes::users::User;
use crate::state::AppState;
use crate::token::{generate_token, hash_token};

// password を持つので Debug / Serialize は付けない（付けるとログや応答にうっかり出せてしまう）
#[derive(Deserialize)]
pub struct RegisterRequest {
    name: String,
    email: String,
    password: String,
}

// 検証と正規化を済ませた入力
struct NewAccount {
    name: String,
    email: String,
    password: String,
}

impl RegisterRequest {
    // 不正なフィールドは全部まとめて 1 回の 422 で返す
    fn validate(self) -> Result<NewAccount, AppError> {
        let mut fields = BTreeMap::new();

        let name = self.name.trim().to_string();
        if !(1..=50).contains(&name.chars().count()) {
            fields.insert(
                "name".to_string(),
                vec!["name must be 1 to 50 characters".to_string()],
            );
        }

        // メールアドレスは前後の空白を取って小文字にそろえる
        let email = self.email.trim().to_lowercase();
        if !email.contains('@') || email.len() > 254 {
            fields.insert(
                "email".to_string(),
                vec!["email must contain @ and be 254 characters or less".to_string()],
            );
        }

        // 下限 15 は NIST SP 800-63B-4（パスワードだけで認証する場合は 15 文字以上）に合わせた値。上限は極端に長い入力を弾くため
        if !(15..=128).contains(&self.password.chars().count()) {
            fields.insert(
                "password".to_string(),
                vec!["password must be 15 to 128 characters".to_string()],
            );
        }

        if fields.is_empty() {
            Ok(NewAccount {
                name,
                email,
                password: self.password,
            })
        } else {
            Err(AppError::Validation(fields))
        }
    }
}

#[derive(Serialize)]
pub struct AuthResponse {
    user: User,
    token: String,
    // 有効期限（UNIX 秒）
    expires_at: i64,
}

// 新しいトークンを作り、ハッシュだけを sessions に保存して (平文のトークン, expires_at) を返す。
// 期限は DB の時計（unixepoch()）に有効期間を足して決める
async fn issue_session(
    conn: &mut SqliteConnection,
    user_id: i64,
    ttl_secs: i64,
) -> Result<(String, i64), AppError> {
    let token = generate_token()
        .map_err(|error| AppError::Internal(format!("generate token failed: {error}")))?;
    let expires_at: i64 = sqlx::query_scalar(
        "INSERT INTO sessions (token_hash, user_id, expires_at) \
         VALUES (?, ?, unixepoch() + ?) RETURNING expires_at",
    )
    .bind(hash_token(&token))
    .bind(user_id)
    .bind(ttl_secs)
    .fetch_one(conn)
    .await?;
    Ok((token, expires_at))
}

pub async fn register(
    State(state): State<AppState>,
    AppJson(payload): AppJson<RegisterRequest>,
) -> Result<(StatusCode, Json<AuthResponse>), AppError> {
    let account = payload.validate()?;

    // ハッシュ計算はトランザクションを開く前に済ませる（計算中にトランザクションを持ち続けない）
    let password_hash = hash_password_blocking(account.password).await?;

    // users・credentials・sessions の 3 つの INSERT を 1 つのトランザクションにする。
    // 途中で `return Err(...)` すると tx が drop され、それまでの INSERT は自動でロールバックされる
    let mut tx = state.pool.begin().await?;
    let user = sqlx::query_as::<_, User>("INSERT INTO users (name) VALUES (?) RETURNING id, name")
        .bind(&account.name)
        .fetch_one(&mut *tx)
        .await?;
    let inserted =
        sqlx::query("INSERT INTO credentials (user_id, email, password_hash) VALUES (?, ?, ?)")
            .bind(user.id)
            .bind(&account.email)
            .bind(&password_hash)
            .execute(&mut *tx)
            .await;
    match inserted {
        Ok(_) => {}
        // email の UNIQUE 制約に違反した = すでに登録されているメールアドレス
        Err(sqlx::Error::Database(error)) if error.is_unique_violation() => {
            return Err(AppError::Conflict("email already registered"));
        }
        Err(error) => return Err(error.into()),
    }
    let (token, expires_at) =
        issue_session(&mut tx, user.id, state.config.session_ttl_secs).await?;
    tx.commit().await?;

    Ok((
        StatusCode::CREATED,
        Json(AuthResponse {
            user,
            token,
            expires_at,
        }),
    ))
}

#[derive(Deserialize)]
pub struct LoginRequest {
    email: String,
    password: String,
}

// 失敗の理由（メールが無い / パスワードが違う）は区別せず、常に同じ文言を返す
const LOGIN_FAILED: &str = "invalid email or password";

#[derive(sqlx::FromRow)]
struct Credential {
    user_id: i64,
    name: String,
    password_hash: String,
}

pub async fn login(
    State(state): State<AppState>,
    AppJson(payload): AppJson<LoginRequest>,
) -> Result<Json<AuthResponse>, AppError> {
    let email = payload.email.trim().to_lowercase();
    // 254 文字を超える email は登録できない（契約 4.3）ので、DB も limiter も見ずに失敗扱いにする。
    // 巨大なキーを limiter に渡すと、そのぶんメモリを食わせられる
    if email.len() > 254 {
        return Err(AppError::Unauthorized(LOGIN_FAILED));
    }
    // email 単位のレート制限。登録済みかどうかに関係なく、同じ email なら同じように数える。
    // DB とハッシュ計算より前に判定するので、制限中のリクエストは argon2 を回さない
    rate_limit::check(&state.limits.login_by_email, &email)?;
    // 登録時の上限（128 文字）を超えるパスワードは正しいはずが無いので、無駄な計算を省くため失敗扱いにする（argon2 の計算時間は入力の長さにほとんど左右されない）
    if payload.password.chars().count() > 128 {
        return Err(AppError::Unauthorized(LOGIN_FAILED));
    }

    let credential = sqlx::query_as::<_, Credential>(
        "SELECT c.user_id, u.name, c.password_hash \
         FROM credentials c JOIN users u ON u.id = c.user_id WHERE c.email = ?",
    )
    .bind(&email)
    .fetch_optional(&state.pool)
    .await?;

    // メールが登録されていなくても、ダミーのハッシュで検証して同じ時間をかける
    let hash = credential.as_ref().map(|c| c.password_hash.clone());
    let verified = verify_password_blocking(payload.password, hash).await?;
    let credential = match credential {
        Some(credential) if verified => credential,
        _ => return Err(AppError::Unauthorized(LOGIN_FAILED)),
    };

    let mut conn = state.pool.acquire().await?;
    // 期限切れのセッションを掃除する簡易版。ログインのたびに全利用者ぶんをまとめて消す
    // （本番では定期ジョブにすることが多い）
    sqlx::query("DELETE FROM sessions WHERE expires_at <= unixepoch()")
        .execute(&mut *conn)
        .await?;
    let (token, expires_at) =
        issue_session(&mut conn, credential.user_id, state.config.session_ttl_secs).await?;
    Ok(Json(AuthResponse {
        user: User {
            id: credential.user_id,
            name: credential.name,
        },
        token,
        expires_at,
    }))
}

#[derive(Serialize)]
pub struct Me {
    id: i64,
    name: String,
    email: String,
}

// 認証が要るかどうかは、引数に AuthUser があるかどうかだけで決まる
pub async fn me(State(state): State<AppState>, user: AuthUser) -> Result<Json<Me>, AppError> {
    let email: String = sqlx::query_scalar("SELECT email FROM credentials WHERE user_id = ?")
        .bind(user.user_id)
        .fetch_one(&state.pool)
        .await?;
    Ok(Json(Me {
        id: user.user_id,
        name: user.name,
        email,
    }))
}

// 今使っているトークンのセッションだけを消す（他の端末のセッションは残る）。
// 消した後は同じトークンで AuthUser の照合に失敗し、401 になる
pub async fn logout(State(state): State<AppState>, user: AuthUser) -> Result<StatusCode, AppError> {
    sqlx::query("DELETE FROM sessions WHERE token_hash = ?")
        .bind(&user.token_hash)
        .execute(&state.pool)
        .await?;
    Ok(StatusCode::NO_CONTENT)
}
