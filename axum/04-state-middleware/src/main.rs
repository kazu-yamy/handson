use axum::{
    Extension, Json, Router,
    extract::{Path, Query, Request, State},
    http::StatusCode,
    middleware::{self, Next},
    response::{IntoResponse, Response},
    routing::{get, post, put},
};
use serde::{Deserialize, Serialize};
use serde_json::json;
use std::collections::HashMap;
use std::sync::{Arc, Mutex};
use std::time::Instant;

#[derive(Clone, Serialize)]
struct User {
    id: u32,
    name: String,
}

#[derive(Deserialize)]
struct CreateUser {
    name: String,
}

#[derive(Clone)]
struct AppState {
    users: Arc<Mutex<Vec<User>>>,
}

#[derive(Clone)]
struct AppConfig {
    app_name: String,
}

#[tokio::main]
async fn main() {
    let state = AppState {
        users: Arc::new(Mutex::new(vec![
            User {
                id: 1,
                name: "alice".to_string(),
            },
            User {
                id: 2,
                name: "bob".to_string(),
            },
        ])),
    };

    let protected_users = Router::new()
        .route("/users", post(create_user))
        .route("/users/{id}", put(update_user))
        .route_layer(middleware::from_fn(require_api_key));

    let app = Router::new()
        .route("/", get(root))
        .route("/health", get(health))
        .route("/users", get(list_users))
        .route("/users/{id}", get(get_user))
        .merge(protected_users)
        .route("/users/{user_id}/posts/{post_id}", get(get_user_post))
        .route("/search", get(search))
        .fallback(not_found)
        .layer(Extension(AppConfig {
            app_name: "Handson Axum".to_string(),
        }))
        .layer(middleware::from_fn(log_request))
        .with_state(state);

    let listener = tokio::net::TcpListener::bind("127.0.0.1:3000")
        .await
        .unwrap();
    println!("listening on {}", listener.local_addr().unwrap());
    axum::serve(listener, app).await.unwrap();
}

async fn log_request(req: Request, next: Next) -> Response {
    let method = req.method().clone();
    let uri = req.uri().clone();
    let start = Instant::now();

    let response = next.run(req).await;

    let elapsed = start.elapsed();
    println!("{method} {uri} -> {} ({elapsed:?})", response.status());
    response
}

async fn require_api_key(req: Request, next: Next) -> Response {
    let is_valid = req
        .headers()
        .get("x-api-key")
        .and_then(|value| value.to_str().ok())
        == Some("secret");

    if !is_valid {
        return (StatusCode::UNAUTHORIZED, "invalid or missing x-api-key").into_response();
    }

    next.run(req).await
}

async fn root(Extension(config): Extension<AppConfig>) -> String {
    format!("Hello from {}", config.app_name)
}

async fn health() -> Json<serde_json::Value> {
    Json(json!({ "status": "ok" }))
}

async fn list_users(State(state): State<AppState>) -> Json<Vec<User>> {
    let users = state.users.lock().unwrap();
    Json(users.clone())
}

async fn create_user(
    State(state): State<AppState>,
    Json(payload): Json<CreateUser>,
) -> (StatusCode, Json<User>) {
    let mut users = state.users.lock().unwrap();
    let user = User {
        id: users.len() as u32 + 1,
        name: payload.name,
    };
    users.push(user.clone());
    (StatusCode::CREATED, Json(user))
}

async fn get_user(
    State(state): State<AppState>,
    Path(id): Path<u32>,
) -> Result<Json<User>, StatusCode> {
    let users = state.users.lock().unwrap();
    users
        .iter()
        .find(|user| user.id == id)
        .cloned()
        .map(Json)
        .ok_or(StatusCode::NOT_FOUND)
}

async fn update_user(
    State(state): State<AppState>,
    Path(id): Path<u32>,
    Json(payload): Json<CreateUser>,
) -> Result<Json<User>, StatusCode> {
    let mut users = state.users.lock().unwrap();
    let user = users
        .iter_mut()
        .find(|user| user.id == id)
        .ok_or(StatusCode::NOT_FOUND)?;
    user.name = payload.name;
    Ok(Json(user.clone()))
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
