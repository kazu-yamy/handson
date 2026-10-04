pub mod auth;
mod health;
pub mod posts;
pub mod users;

use axum::{
    Router,
    http::StatusCode,
    middleware,
    response::Response,
    routing::{get, patch, post, put},
};

use crate::error::error_response;
use crate::middleware::{log_request, require_api_key};
use crate::state::AppState;

pub fn router(state: AppState) -> Router {
    let protected_users = Router::new()
        .route("/users", post(users::create_user))
        .route(
            "/users/{id}",
            put(users::update_user).delete(users::delete_user),
        )
        .route_layer(middleware::from_fn_with_state(
            state.clone(),
            require_api_key,
        ));

    let protected_posts = Router::new()
        .route("/posts", post(posts::create_post))
        .route(
            "/posts/{id}",
            patch(posts::update_post).delete(posts::delete_post),
        )
        .route_layer(middleware::from_fn_with_state(
            state.clone(),
            require_api_key,
        ));

    Router::new()
        .route("/health", get(health::health))
        .route("/auth/register", post(auth::register))
        .route("/auth/login", post(auth::login))
        .route("/users", get(users::list_users))
        .route("/users/{id}", get(users::get_user))
        .route("/posts", get(posts::list_posts))
        .route("/posts/{id}", get(posts::get_post))
        .route("/posts/{id}/like", post(posts::like_post))
        .merge(protected_users)
        .merge(protected_posts)
        .fallback(not_found)
        .layer(middleware::from_fn(log_request))
        .with_state(state)
}

async fn not_found() -> Response {
    error_response(StatusCode::NOT_FOUND, "not_found", "route not found")
}
