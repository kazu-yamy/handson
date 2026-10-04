mod health;
pub mod users;

use axum::{
    Router,
    http::StatusCode,
    middleware,
    response::Response,
    routing::{get, post, put},
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

    Router::new()
        .route("/health", get(health::health))
        .route("/users", get(users::list_users))
        .route("/users/{id}", get(users::get_user))
        .merge(protected_users)
        .fallback(not_found)
        .layer(middleware::from_fn(log_request))
        .with_state(state)
}

async fn not_found() -> Response {
    error_response(StatusCode::NOT_FOUND, "not_found", "route not found")
}
