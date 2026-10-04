use axum::{
    extract::FromRequestParts,
    http::{header::AUTHORIZATION, request::Parts},
};

use crate::error::AppError;
use crate::state::AppState;
use crate::token::hash_token;

// ハンドラの引数に置くだけで「ログイン済みであること」を要求する型。
// 認証に失敗すると、ハンドラの本体は呼ばれずに 401 が返る。
// Debug は付けない（token_hash をログに出す事故を防ぐ）
pub struct AuthUser {
    pub user_id: i64,
    pub name: String,
    // ログアウトで「このトークンのセッションだけ」を消すために持つ（トークンそのものは持たない）
    pub token_hash: Vec<u8>,
}

// 無い・形式が違う・見つからない・期限切れのどれでも同じ文言にする（理由を区別して教えない）
const UNAUTHORIZED: &str = "missing or invalid bearer token";

// `Authorization: Bearer <token>` からトークンを取り出す。スキーム名の大文字小文字は区別しない（RFC 9110）。
// スキームとトークンの間の空白は 1 個以上（RFC 9110 の `1*SP`）なので、先頭の空白をすべて取り除く
fn bearer_token(parts: &Parts) -> Option<&str> {
    let value = parts.headers.get(AUTHORIZATION)?.to_str().ok()?;
    let (scheme, token) = value.split_once(' ')?;
    let token = token.trim_start_matches(' ');
    if scheme.eq_ignore_ascii_case("bearer") && !token.is_empty() {
        Some(token)
    } else {
        None
    }
}

#[derive(sqlx::FromRow)]
struct SessionUser {
    user_id: i64,
    name: String,
}

// Rejection に AppError を使うので、失敗は他のハンドラと同じ形式の 401 になる
impl FromRequestParts<AppState> for AuthUser {
    type Rejection = AppError;

    async fn from_request_parts(
        parts: &mut Parts,
        state: &AppState,
    ) -> Result<Self, Self::Rejection> {
        let token = bearer_token(parts).ok_or(AppError::Unauthorized(UNAUTHORIZED))?;
        let token_hash = hash_token(token);

        // 期限の判定は DB の時計（unixepoch()）で行う。アプリ側の時刻とずれる心配がない
        let session = sqlx::query_as::<_, SessionUser>(
            "SELECT u.id AS user_id, u.name \
             FROM sessions s JOIN users u ON u.id = s.user_id \
             WHERE s.token_hash = ? AND s.expires_at > unixepoch()",
        )
        .bind(&token_hash)
        .fetch_optional(&state.pool)
        .await?
        .ok_or(AppError::Unauthorized(UNAUTHORIZED))?;

        Ok(AuthUser {
            user_id: session.user_id,
            name: session.name,
            token_hash,
        })
    }
}
