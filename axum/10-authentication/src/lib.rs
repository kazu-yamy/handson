pub mod config;
pub mod error;
pub mod middleware;
pub mod password;
pub mod routes;
pub mod state;
pub mod token;

use axum::Router;

use crate::state::AppState;

pub fn build_app(state: AppState) -> Router {
    routes::router(state)
}
