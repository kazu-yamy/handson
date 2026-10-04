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
use crate::rate_limit::{limit_auth_by_ip, limit_like_by_ip};
use crate::state::AppState;

pub fn router(state: AppState) -> Router {
    Router::new()
        .route("/health", get(health::health))
        // route_layer を MethodRouter（post(..) の戻り値）に掛けると、そのルートの、そのメソッドだけに効く。
        // Router に掛けると、それまでに登録した全ルートに掛かってしまう
        .route(
            "/auth/register",
            post(auth::register).route_layer(middleware::from_fn_with_state(
                state.clone(),
                limit_auth_by_ip,
            )),
        )
        .route(
            "/auth/login",
            post(auth::login).route_layer(middleware::from_fn_with_state(
                state.clone(),
                limit_auth_by_ip,
            )),
        )
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
        .route(
            "/posts/{id}/like",
            post(posts::like_post).route_layer(middleware::from_fn_with_state(
                state.clone(),
                limit_like_by_ip,
            )),
        )
        // method_not_allowed_fallback は「呼んだ時点までに登録されたルート」にだけ効く。.route() を全部書いた後に呼ぶ
        .method_not_allowed_fallback(method_not_allowed)
        .fallback(not_found)
        .with_state(state)
}

// ルートはあるがメソッドが無いとき。axum が付けた `Allow` ヘッダーはそのまま残る
async fn method_not_allowed() -> Response {
    error_response(
        StatusCode::METHOD_NOT_ALLOWED,
        "method_not_allowed",
        "method not allowed",
    )
}

async fn not_found() -> Response {
    error_response(StatusCode::NOT_FOUND, "not_found", "route not found")
}
