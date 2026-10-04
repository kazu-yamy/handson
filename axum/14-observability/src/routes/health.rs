use axum::{
    Json,
    extract::State,
    http::StatusCode,
    response::{IntoResponse, Response},
};
use serde_json::json;

use crate::error::{code_for_status, error_response};
use crate::state::AppState;

// 生存確認（liveness）。プロセスがリクエストに答えられるかだけを返す。DB には触れない
pub async fn health() -> Json<serde_json::Value> {
    Json(json!({ "status": "ok" }))
}

// 準備確認（readiness）。DB に実際にクエリが通るときだけ 200 を返す。
// ロードバランサーやオーケストレーターは、これが 503 の間はこのインスタンスにリクエストを回さない
pub async fn ready(State(state): State<AppState>) -> Response {
    match sqlx::query("SELECT 1").execute(&state.pool).await {
        Ok(_) => Json(json!({ "status": "ok" })).into_response(),
        Err(error) => {
            // 原因（接続エラーの詳細など）はログにだけ出し、応答には固定の文言だけを返す
            tracing::error!(%error, "readiness check failed");
            let status = StatusCode::SERVICE_UNAVAILABLE;
            error_response(status, code_for_status(status), "service unavailable")
        }
    }
}
