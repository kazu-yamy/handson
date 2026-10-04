use axum::{
    Json,
    extract::rejection::{JsonRejection, PathRejection, QueryRejection},
    http::{
        HeaderValue, StatusCode,
        header::{RETRY_AFTER, WWW_AUTHENTICATE},
    },
    response::{IntoResponse, Response},
};
use serde::Serialize;
use std::collections::BTreeMap;

#[derive(Serialize)]
pub struct ErrorBody {
    pub error: ErrorDetail,
}

#[derive(Serialize)]
pub struct ErrorDetail {
    pub code: &'static str,
    pub message: String,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub fields: Option<BTreeMap<String, Vec<String>>>,
}

pub fn error_response(
    status: StatusCode,
    code: &'static str,
    message: impl Into<String>,
) -> Response {
    let body = ErrorBody {
        error: ErrorDetail {
            code,
            message: message.into(),
            fields: None,
        },
    };
    (status, Json(body)).into_response()
}

// ステータスに対応する契約の `code`（API 契約 4.4）。AppError の変換と、層が返した本文の無いエラーの補完で使う
pub fn code_for_status(status: StatusCode) -> &'static str {
    match status {
        StatusCode::UNAUTHORIZED => "unauthorized",
        StatusCode::NOT_FOUND => "not_found",
        StatusCode::METHOD_NOT_ALLOWED => "method_not_allowed",
        StatusCode::REQUEST_TIMEOUT => "timeout",
        StatusCode::CONFLICT => "conflict",
        StatusCode::PAYLOAD_TOO_LARGE => "payload_too_large",
        StatusCode::UNSUPPORTED_MEDIA_TYPE => "unsupported_media_type",
        StatusCode::UNPROCESSABLE_ENTITY => "validation_failed",
        StatusCode::TOO_MANY_REQUESTS => "rate_limited",
        StatusCode::SERVICE_UNAVAILABLE => "unavailable",
        status if status.is_server_error() => "internal",
        _ => "bad_request",
    }
}

// 層が本文の無いエラーを返したときに補う、ステータスごとの固定の文言
pub fn message_for_status(status: StatusCode) -> &'static str {
    match status {
        StatusCode::NOT_FOUND => "route not found",
        StatusCode::METHOD_NOT_ALLOWED => "method not allowed",
        StatusCode::REQUEST_TIMEOUT => "request timed out",
        StatusCode::TOO_MANY_REQUESTS => "too many requests",
        StatusCode::PAYLOAD_TOO_LARGE => "request body is too large",
        status if status.is_server_error() => "internal server error",
        _ => "request failed",
    }
}

#[derive(Debug)]
pub enum AppError {
    NotFound(&'static str),
    Conflict(&'static str),
    Unauthorized(&'static str),
    Validation(BTreeMap<String, Vec<String>>),
    // レート制限。次に通るまでの秒数を Retry-After に入れる
    RateLimited { retry_after_secs: u64 },
    Database(sqlx::Error),
    Internal(String),
    JsonRejection(JsonRejection),
    PathRejection(PathRejection),
    QueryRejection(QueryRejection),
}

impl AppError {
    pub fn field(name: &str, message: &str) -> Self {
        AppError::Validation(BTreeMap::from([(
            name.to_string(),
            vec![message.to_string()],
        )]))
    }
}

impl IntoResponse for AppError {
    fn into_response(self) -> Response {
        match self {
            AppError::NotFound(message) => {
                error_response(StatusCode::NOT_FOUND, "not_found", message)
            }
            AppError::Conflict(message) => {
                error_response(StatusCode::CONFLICT, "conflict", message)
            }
            AppError::Unauthorized(message) => {
                let mut response =
                    error_response(StatusCode::UNAUTHORIZED, "unauthorized", message);
                response
                    .headers_mut()
                    .insert(WWW_AUTHENTICATE, HeaderValue::from_static("Bearer"));
                response
            }
            AppError::Validation(fields) => {
                let body = ErrorBody {
                    error: ErrorDetail {
                        code: "validation_failed",
                        message: "invalid request".to_string(),
                        fields: Some(fields),
                    },
                };
                (StatusCode::UNPROCESSABLE_ENTITY, Json(body)).into_response()
            }
            AppError::RateLimited { retry_after_secs } => {
                let mut response = error_response(
                    StatusCode::TOO_MANY_REQUESTS,
                    "rate_limited",
                    "too many requests",
                );
                response
                    .headers_mut()
                    .insert(RETRY_AFTER, HeaderValue::from(retry_after_secs));
                response
            }
            AppError::Database(error) => {
                tracing::error!(%error, "database error");
                error_response(
                    StatusCode::INTERNAL_SERVER_ERROR,
                    "internal",
                    "internal server error",
                )
            }
            AppError::Internal(message) => {
                tracing::error!(%message, "internal error");
                error_response(
                    StatusCode::INTERNAL_SERVER_ERROR,
                    "internal",
                    "internal server error",
                )
            }
            // 拒否の message は、ステータスごとの固定の文言にする。
            // rejection.body_text() は axum の内部の文言で、送られた値（パスワードなど）を含むことがあるので、応答にも出さない
            AppError::JsonRejection(rejection) => {
                let status = rejection.status();
                let message = match status {
                    StatusCode::UNPROCESSABLE_ENTITY => {
                        "request body does not match the expected format"
                    }
                    StatusCode::UNSUPPORTED_MEDIA_TYPE => "content-type must be application/json",
                    StatusCode::PAYLOAD_TOO_LARGE => "request body is too large",
                    _ if matches!(rejection, JsonRejection::JsonSyntaxError(_)) => {
                        "request body is not valid JSON"
                    }
                    _ => "failed to read request body",
                };
                error_response(status, code_for_status(status), message)
            }
            AppError::PathRejection(rejection) => {
                let status = rejection.status();
                if status.is_server_error() {
                    // ルート定義の誤り（パラメーターの数や名前の不一致）。利用者の入力は含まないのでログに残してよい
                    tracing::error!(detail = %rejection.body_text(), "path rejection");
                    return error_response(status, "internal", "internal server error");
                }
                error_response(status, code_for_status(status), "invalid path parameter")
            }
            AppError::QueryRejection(rejection) => {
                let status = rejection.status();
                error_response(status, code_for_status(status), "invalid query string")
            }
        }
    }
}

impl From<sqlx::Error> for AppError {
    fn from(error: sqlx::Error) -> Self {
        AppError::Database(error)
    }
}

impl From<JsonRejection> for AppError {
    fn from(rejection: JsonRejection) -> Self {
        AppError::JsonRejection(rejection)
    }
}

impl From<PathRejection> for AppError {
    fn from(rejection: PathRejection) -> Self {
        AppError::PathRejection(rejection)
    }
}

impl From<QueryRejection> for AppError {
    fn from(rejection: QueryRejection) -> Self {
        AppError::QueryRejection(rejection)
    }
}
