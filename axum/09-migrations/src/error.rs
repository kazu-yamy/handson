use axum::{
    Json,
    extract::rejection::{JsonRejection, QueryRejection},
    http::StatusCode,
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

pub enum AppError {
    NotFound(&'static str),
    Validation(BTreeMap<String, Vec<String>>),
    Database(sqlx::Error),
    JsonRejection(JsonRejection),
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
            AppError::Database(error) => {
                eprintln!("database error: {error}");
                error_response(
                    StatusCode::INTERNAL_SERVER_ERROR,
                    "internal",
                    "internal server error",
                )
            }
            AppError::JsonRejection(rejection) => {
                let status = rejection.status();
                let code = match status {
                    StatusCode::UNPROCESSABLE_ENTITY => "validation_failed",
                    StatusCode::UNSUPPORTED_MEDIA_TYPE => "unsupported_media_type",
                    StatusCode::PAYLOAD_TOO_LARGE => "payload_too_large",
                    _ => "bad_request",
                };
                error_response(status, code, rejection.body_text())
            }
            AppError::QueryRejection(rejection) => {
                error_response(rejection.status(), "bad_request", rejection.body_text())
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

impl From<QueryRejection> for AppError {
    fn from(rejection: QueryRejection) -> Self {
        AppError::QueryRejection(rejection)
    }
}
