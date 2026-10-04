pub mod auth_user;
pub mod config;
pub mod error;
pub mod extract;
pub mod layers;
pub mod password;
pub mod routes;
pub mod state;
pub mod token;

use axum::Router;

use crate::state::AppState;

pub fn build_app(state: AppState) -> Router {
    let config = state.config.clone();
    layers::apply(routes::router(state), &config)
}
