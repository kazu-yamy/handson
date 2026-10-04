use axum::{Json, extract::State};
use serde::Serialize;

use crate::error::AppError;
use crate::extract::AppPath;
use crate::state::AppState;

#[derive(Clone, Serialize, sqlx::FromRow)]
pub struct User {
    pub id: i64,
    pub name: String,
}

pub async fn list_users(State(state): State<AppState>) -> Result<Json<Vec<User>>, AppError> {
    let users = sqlx::query_as::<_, User>("SELECT id, name FROM users ORDER BY id")
        .fetch_all(&state.pool)
        .await?;
    Ok(Json(users))
}

pub async fn get_user(
    State(state): State<AppState>,
    AppPath(id): AppPath<i64>,
) -> Result<Json<User>, AppError> {
    let user = sqlx::query_as::<_, User>("SELECT id, name FROM users WHERE id = ?")
        .bind(id)
        .fetch_optional(&state.pool)
        .await?;
    user.map(Json).ok_or(AppError::NotFound("user not found"))
}
