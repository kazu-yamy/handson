use axum::{
    Router,
    extract::{Path, Query},
    http::StatusCode,
    routing::get,
};
use std::collections::HashMap;

#[tokio::main]
async fn main() {
    let app = Router::new()
        .route("/", get(root))
        .route("/health", get(health))
        .route("/users", get(list_users).post(create_user))
        .route("/users/{id}", get(get_user))
        .route("/users/{user_id}/posts/{post_id}", get(get_user_post))
        .route("/search", get(search))
        .fallback(not_found);

    let listener = tokio::net::TcpListener::bind("127.0.0.1:3000")
        .await
        .unwrap();
    println!("listening on {}", listener.local_addr().unwrap());
    axum::serve(listener, app).await.unwrap();
}

async fn root() -> &'static str {
    "Hello, Axum!"
}

async fn health() -> &'static str {
    "ok"
}

async fn list_users() -> &'static str {
    "list users"
}

async fn create_user() -> &'static str {
    "create user"
}

async fn get_user(Path(id): Path<u32>) -> String {
    format!("user {id}")
}

async fn get_user_post(Path((user_id, post_id)): Path<(u32, u32)>) -> String {
    format!("user {user_id} post {post_id}")
}

async fn search(Query(params): Query<HashMap<String, String>>) -> String {
    format!("search params: {params:?}")
}

async fn not_found() -> (StatusCode, &'static str) {
    (StatusCode::NOT_FOUND, "Not Found")
}
