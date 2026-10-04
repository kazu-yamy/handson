use axum::{
    Json,
    extract::{Path, State, rejection::JsonRejection},
    http::StatusCode,
};
use serde::{Deserialize, Serialize};

use crate::error::AppError;
use crate::state::AppState;

#[derive(Clone, Serialize, sqlx::FromRow)]
pub struct User {
    pub id: i64,
    pub name: String,
}

#[derive(Deserialize)]
pub struct CreateUser {
    name: String,
}

impl CreateUser {
    fn validate(&self) -> Result<(), AppError> {
        let name = self.name.trim();
        if name.is_empty() {
            return Err(AppError::field("name", "name must not be empty"));
        }
        if name.chars().count() > 50 {
            return Err(AppError::field(
                "name",
                "name must be 50 characters or less",
            ));
        }
        Ok(())
    }
}

pub async fn list_users(State(state): State<AppState>) -> Result<Json<Vec<User>>, AppError> {
    let users = sqlx::query_as::<_, User>("SELECT id, name FROM users ORDER BY id")
        .fetch_all(&state.pool)
        .await?;
    Ok(Json(users))
}

pub async fn create_user(
    State(state): State<AppState>,
    payload: Result<Json<CreateUser>, JsonRejection>,
) -> Result<(StatusCode, Json<User>), AppError> {
    let Json(payload) = payload?;
    payload.validate()?;
    let user = sqlx::query_as::<_, User>("INSERT INTO users (name) VALUES (?) RETURNING id, name")
        .bind(&payload.name)
        .fetch_one(&state.pool)
        .await?;
    Ok((StatusCode::CREATED, Json(user)))
}

pub async fn get_user(
    State(state): State<AppState>,
    Path(id): Path<i64>,
) -> Result<Json<User>, AppError> {
    let user = sqlx::query_as::<_, User>("SELECT id, name FROM users WHERE id = ?")
        .bind(id)
        .fetch_optional(&state.pool)
        .await?;
    user.map(Json).ok_or(AppError::NotFound("user not found"))
}

pub async fn update_user(
    State(state): State<AppState>,
    Path(id): Path<i64>,
    payload: Result<Json<CreateUser>, JsonRejection>,
) -> Result<Json<User>, AppError> {
    let Json(payload) = payload?;
    payload.validate()?;
    let user =
        sqlx::query_as::<_, User>("UPDATE users SET name = ? WHERE id = ? RETURNING id, name")
            .bind(&payload.name)
            .bind(id)
            .fetch_optional(&state.pool)
            .await?;
    user.map(Json).ok_or(AppError::NotFound("user not found"))
}

pub async fn delete_user(
    State(state): State<AppState>,
    Path(id): Path<i64>,
) -> Result<StatusCode, AppError> {
    let result = sqlx::query("DELETE FROM users WHERE id = ?")
        .bind(id)
        .execute(&state.pool)
        .await?;
    if result.rows_affected() == 0 {
        Err(AppError::NotFound("user not found"))
    } else {
        Ok(StatusCode::NO_CONTENT)
    }
}
