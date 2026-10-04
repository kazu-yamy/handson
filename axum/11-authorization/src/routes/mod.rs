pub mod auth;
mod health;
pub mod posts;
pub mod users;

use axum::{
    Router,
    http::StatusCode,
    middleware,
    response::Response,
    routing::{get, post},
};

use crate::error::error_response;
use crate::middleware::log_request;
use crate::state::AppState;

pub fn router(state: AppState) -> Router {
    Router::new()
        .route("/health", get(health::health))
        .route("/auth/register", post(auth::register))
        .route("/auth/login", post(auth::login))
        .route("/auth/logout", post(auth::logout))
        .route("/auth/me", get(auth::me))
        .route("/users", get(users::list_users))
        .route("/users/{id}", get(users::get_user))
        .route("/posts", get(posts::list_posts).post(posts::create_post))
        .route(
            "/posts/{id}",
            get(posts::get_post)
                .patch(posts::update_post)
                .delete(posts::delete_post),
        )
        .route("/posts/{id}/like", post(posts::like_post))
        .fallback(not_found)
        .layer(middleware::from_fn(log_request))
        .with_state(state)
}

async fn not_found() -> Response {
    error_response(StatusCode::NOT_FOUND, "not_found", "route not found")
}
