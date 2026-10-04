use axum::{
    Router,
    extract::{DefaultBodyLimit, Request},
    http::{
        StatusCode,
        header::{CONTENT_LENGTH, CONTENT_TYPE},
    },
    middleware::{self, Next},
    response::Response,
};
use std::time::{Duration, Instant};
use tower_http::timeout::TimeoutLayer;

use crate::config::Config;
use crate::error::{code_for_status, error_response, message_for_status};

// リクエスト本文の上限（API 契約 4.8）。axum の既定は 2 MiB で、この API の本文（記事の本文は 5000 文字まで）には大きすぎる
pub const MAX_BODY_BYTES: usize = 64 * 1024;

// Router::layer は「後に書いたものほど外側」になる。リクエストは外側から内側へ、応答は内側から外側へ通る
pub fn apply(router: Router, config: &Config) -> Router {
    router
        // 一番内側: 本文を読むエクストラクタ（Json など）にだけ効く。超えると JsonRejection 経由で 413 になる
        .layer(DefaultBodyLimit::max(MAX_BODY_BYTES))
        // 処理に時間がかかりすぎたら 408 にする。この層が返す応答は本文が空
        .layer(TimeoutLayer::with_status_code(
            StatusCode::REQUEST_TIMEOUT,
            config.request_timeout,
        ))
        // 本文の無いエラー応答を契約の JSON に補う。TimeoutLayer より外側に置かないと 408 が素通りする
        .layer(middleware::map_response(fill_empty_error_body))
        // 一番外側: タイムアウトで捨てられたリクエストもログに残す
        .layer(middleware::from_fn(log_request))
}

// ステータスが 4xx / 5xx で content-type が無い（＝どの層も本文を作っていない）応答を、契約の JSON に置き換える。
// 将来、本文の空のエラーを返す層を足しても、形がそろう安全網。Allow など元のヘッダーは残す
async fn fill_empty_error_body(response: Response) -> Response {
    let status = response.status();
    let is_error = status.is_client_error() || status.is_server_error();
    if !is_error || response.headers().contains_key(CONTENT_TYPE) {
        return response;
    }
    let (mut parts, _) = response.into_parts();
    parts.headers.remove(CONTENT_LENGTH);
    let mut filled = error_response(status, code_for_status(status), message_for_status(status));
    filled.headers_mut().extend(parts.headers);
    filled
}

async fn log_request(req: Request, next: Next) -> Response {
    let method = req.method().clone();
    let uri = req.uri().clone();
    let start = Instant::now();

    let response = next.run(req).await;

    let elapsed: Duration = start.elapsed();
    println!("{method} {uri} -> {} ({elapsed:?})", response.status());
    response
}
