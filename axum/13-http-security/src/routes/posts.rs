use axum::{Json, extract::State, http::StatusCode};
use serde::{Deserialize, Serialize};
use std::collections::BTreeMap;

use crate::auth_user::AuthUser;
use crate::error::AppError;
use crate::extract::{AppJson, AppPath, AppQuery};
use crate::state::AppState;

#[derive(Clone, Serialize, sqlx::FromRow)]
pub struct Post {
    id: i64,
    author_id: i64,
    title: String,
    body: String,
    like_count: i64,
    created_at: String,
    updated_at: String,
}

#[derive(Deserialize)]
pub struct CreatePost {
    title: String,
    body: String,
}

#[derive(Deserialize)]
pub struct UpdatePost {
    title: Option<String>,
    body: Option<String>,
}

#[derive(Deserialize)]
pub struct ListParams {
    limit: Option<i64>,
    cursor: Option<i64>,
    author_id: Option<i64>,
}

#[derive(Serialize)]
pub struct PostPage {
    items: Vec<Post>,
    next_cursor: Option<i64>,
}

#[derive(Serialize, sqlx::FromRow)]
pub struct LikeResponse {
    id: i64,
    like_count: i64,
}

const DEFAULT_LIMIT: i64 = 20;
const MAX_LIMIT: i64 = 100;

// 指定されたフィールドだけを検証し、不正なものは全部まとめて 1 回の 422 で返す
fn validate(title: Option<&str>, body: Option<&str>) -> Result<(), AppError> {
    let mut fields = BTreeMap::new();
    if let Some(title) = title
        && !(1..=100).contains(&title.trim().chars().count())
    {
        fields.insert(
            "title".to_string(),
            vec!["title must be 1 to 100 characters".to_string()],
        );
    }
    if let Some(body) = body
        && !(1..=5000).contains(&body.trim().chars().count())
    {
        fields.insert(
            "body".to_string(),
            vec!["body must be 1 to 5000 characters".to_string()],
        );
    }
    if fields.is_empty() {
        Ok(())
    } else {
        Err(AppError::Validation(fields))
    }
}

// 著者はリクエストの本文ではなく、トークンの持ち主（AuthUser）で決める。
// 本文に author_id が付いていても、CreatePost に無いフィールドなので読み捨てられる。
// Json は本文を消費するので引数の最後。AuthUser はその前に置く（認証が先に走る）
pub async fn create_post(
    State(state): State<AppState>,
    user: AuthUser,
    AppJson(payload): AppJson<CreatePost>,
) -> Result<(StatusCode, Json<Post>), AppError> {
    validate(Some(&payload.title), Some(&payload.body))?;
    let post = sqlx::query_as::<_, Post>(
        "INSERT INTO posts (author_id, title, body) VALUES (?, ?, ?) \
         RETURNING id, author_id, title, body, like_count, created_at, updated_at",
    )
    .bind(user.user_id)
    .bind(payload.title.trim())
    .bind(payload.body.trim())
    .fetch_one(&state.pool)
    .await?;
    Ok((StatusCode::CREATED, Json(post)))
}

pub async fn list_posts(
    State(state): State<AppState>,
    AppQuery(params): AppQuery<ListParams>,
) -> Result<Json<PostPage>, AppError> {
    let limit = params.limit.unwrap_or(DEFAULT_LIMIT);
    if !(1..=MAX_LIMIT).contains(&limit) {
        return Err(AppError::field("limit", "limit must be 1 to 100"));
    }
    // 次のページがあるか判定するため、limit より 1 件多く取る
    // 新しい順（id 降順）なので、cursor より小さい id を取る
    let mut items = sqlx::query_as::<_, Post>(
        "SELECT id, author_id, title, body, like_count, created_at, updated_at \
         FROM posts \
         WHERE (?1 IS NULL OR id < ?1) AND (?2 IS NULL OR author_id = ?2) \
         ORDER BY id DESC LIMIT ?3",
    )
    .bind(params.cursor)
    .bind(params.author_id)
    .bind(limit + 1)
    .fetch_all(&state.pool)
    .await?;
    let next_cursor = if items.len() as i64 > limit {
        items.truncate(limit as usize);
        items.last().map(|post| post.id)
    } else {
        None
    };
    Ok(Json(PostPage { items, next_cursor }))
}

pub async fn get_post(
    State(state): State<AppState>,
    AppPath(id): AppPath<i64>,
) -> Result<Json<Post>, AppError> {
    let post = sqlx::query_as::<_, Post>(
        "SELECT id, author_id, title, body, like_count, created_at, updated_at \
         FROM posts WHERE id = ?",
    )
    .bind(id)
    .fetch_optional(&state.pool)
    .await?;
    post.map(Json).ok_or(AppError::NotFound("post not found"))
}

// 所有者だけが更新できる。「id が一致する」だけでなく「author_id がトークンの持ち主と一致する」
// を WHERE に入れるので、他人の記事は「存在しない記事」と区別がつかず、どちらも 404 になる
pub async fn update_post(
    State(state): State<AppState>,
    user: AuthUser,
    AppPath(id): AppPath<i64>,
    AppJson(payload): AppJson<UpdatePost>,
) -> Result<Json<Post>, AppError> {
    validate(payload.title.as_deref(), payload.body.as_deref())?;
    // 未指定（None → NULL）の項目は COALESCE で今の値のまま残す
    let post = sqlx::query_as::<_, Post>(
        "UPDATE posts SET title = COALESCE(?1, title), body = COALESCE(?2, body), \
         updated_at = strftime('%Y-%m-%dT%H:%M:%SZ', 'now') \
         WHERE id = ?3 AND author_id = ?4 \
         RETURNING id, author_id, title, body, like_count, created_at, updated_at",
    )
    .bind(payload.title.as_deref().map(str::trim))
    .bind(payload.body.as_deref().map(str::trim))
    .bind(id)
    .bind(user.user_id)
    .fetch_optional(&state.pool)
    .await?;
    post.map(Json).ok_or(AppError::NotFound("post not found"))
}

pub async fn delete_post(
    State(state): State<AppState>,
    user: AuthUser,
    AppPath(id): AppPath<i64>,
) -> Result<StatusCode, AppError> {
    let result = sqlx::query("DELETE FROM posts WHERE id = ? AND author_id = ?")
        .bind(id)
        .bind(user.user_id)
        .execute(&state.pool)
        .await?;
    if result.rows_affected() == 0 {
        Err(AppError::NotFound("post not found"))
    } else {
        Ok(StatusCode::NO_CONTENT)
    }
}

pub async fn like_post(
    State(state): State<AppState>,
    AppPath(id): AppPath<i64>,
) -> Result<Json<LikeResponse>, AppError> {
    // 「読んでから +1 して書く」のではなく、1 文の UPDATE で原子的に増やす
    let liked = sqlx::query_as::<_, LikeResponse>(
        "UPDATE posts SET like_count = like_count + 1 WHERE id = ? RETURNING id, like_count",
    )
    .bind(id)
    .fetch_optional(&state.pool)
    .await?;
    liked.map(Json).ok_or(AppError::NotFound("post not found"))
}
