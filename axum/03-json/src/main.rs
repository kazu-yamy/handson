use axum::{
    Json, Router,
    extract::{Path, Query},
    http::StatusCode,
    routing::get,
};
use serde::{Deserialize, Serialize};
use serde_json::json;
use std::collections::HashMap;

#[derive(Serialize)]
struct User {
    id: u32,
    name: String,
}

#[derive(Deserialize)]
struct CreateUser {
    name: String,
}

#[tokio::main]
async fn main() {
    let app = Router::new()
        .route("/", get(root))
        .route("/health", get(health))
        .route("/users", get(list_users).post(create_user))
        .route("/users/{id}", get(get_user).put(update_user))
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

async fn health() -> Json<serde_json::Value> {
    Json(json!({ "status": "ok" }))
}

async fn list_users() -> Json<Vec<User>> {
    Json(vec![
        User {
            id: 1,
            name: "alice".to_string(),
        },
        User {
            id: 2,
            name: "bob".to_string(),
        },
    ])
}

async fn create_user(Json(payload): Json<CreateUser>) -> (StatusCode, Json<User>) {
    let user = User {
        id: 1,
        name: payload.name,
    };
    (StatusCode::CREATED, Json(user))
}

async fn get_user(Path(id): Path<u32>) -> Json<User> {
    Json(User {
        id,
        name: "alice".to_string(),
    })
}

async fn update_user(Path(id): Path<u32>, Json(payload): Json<CreateUser>) -> Json<User> {
    Json(User {
        id,
        name: payload.name,
    })
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
