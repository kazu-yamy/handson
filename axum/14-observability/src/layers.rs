use axum::{
    Router,
    extract::{DefaultBodyLimit, Request},
    http::{
        HeaderName, HeaderValue, Method, StatusCode,
        header::{
            AUTHORIZATION, CACHE_CONTROL, CONTENT_LENGTH, CONTENT_SECURITY_POLICY, CONTENT_TYPE,
            REFERRER_POLICY, RETRY_AFTER, X_CONTENT_TYPE_OPTIONS,
        },
    },
    middleware,
    response::Response,
};
use std::time::Duration;
use tower_http::{
    cors::{AllowOrigin, CorsLayer},
    request_id::{MakeRequestUuid, PropagateRequestIdLayer, SetRequestIdLayer},
    sensitive_headers::SetSensitiveRequestHeadersLayer,
    set_header::SetResponseHeaderLayer,
    timeout::TimeoutLayer,
    trace::{DefaultOnResponse, TraceLayer},
};
use tracing::{Level, Span};

use crate::config::Config;
use crate::error::{code_for_status, error_response, message_for_status};

// リクエスト本文の上限（API 契約 4.8）。axum の既定は 2 MiB で、この API の本文（記事の本文は 5000 文字まで）には大きすぎる
pub const MAX_BODY_BYTES: usize = 64 * 1024;

// Router::layer は「後に書いたものほど外側」になる。リクエストは外側から内側へ、応答は内側から外側へ通る
pub fn apply(router: Router, config: &Config) -> Router {
    let router = router
        // 一番内側: 本文を読むエクストラクタ（Json など）にだけ効く。超えると JsonRejection 経由で 413 になる
        .layer(DefaultBodyLimit::max(MAX_BODY_BYTES))
        // 処理に時間がかかりすぎたら 408 にする。この層が返す応答は本文が空
        .layer(TimeoutLayer::with_status_code(
            StatusCode::REQUEST_TIMEOUT,
            config.request_timeout,
        ))
        // 本文の無いエラー応答を契約の JSON に補う。TimeoutLayer より外側に置かないと 408 が素通りする
        .layer(middleware::map_response(fill_empty_error_body));

    // CORS は設定があるときだけ掛ける。ログの内側、エラー補完の外側に置くので、
    // 429・413・408 などのエラー応答にも access-control-* ヘッダーが付く
    // （ブラウザは access-control-allow-origin の無いエラー応答を JavaScript に渡さない）
    let router = if config.cors_allowed_origins.is_empty() {
        router
    } else {
        router.layer(cors(config))
    };

    // セキュリティヘッダーは全応答に付ける。CORS の外側に置くので、
    // 内側を呼ばずに返す CORS のプリフライト応答や、本文を補った 408、ルーターが返す 404・405 にも付く
    let router = security_headers(router);

    // ここから外側は「見える化」の層。内側から順に書く（後に書いたものほど外側）
    router
        // リクエストごとの span を作り、応答が返ったときに 1 行ログを出す。
        // セキュリティヘッダーの外側に置くので、タイムアウトで捨てられたリクエストや CORS のプリフライトも記録される
        .layer(
            TraceLayer::new_for_http()
                .make_span_with(make_span)
                .on_response(DefaultOnResponse::new().level(Level::INFO)),
        )
        // リクエストの x-request-id を、応答にも同じ値で付ける。Trace の外側に置くので、内側のどの層が返した応答にも付く
        .layer(PropagateRequestIdLayer::x_request_id())
        // x-request-id が無いリクエストには UUID を割り当てる。Trace より外側に置かないと、span に ID が入らない
        .layer(SetRequestIdLayer::x_request_id(MakeRequestUuid))
        // authorization ヘッダーの値を「機密」と印付けする。Debug 出力では値の代わりに Sensitive と出る。
        // Trace より外側に置かないと、Trace がヘッダーを読む時点で印が付いていない
        .layer(SetSensitiveRequestHeadersLayer::new([AUTHORIZATION]))
}

const X_REQUEST_ID: HeaderName = HeaderName::from_static("x-request-id");

// リクエスト 1 件ぶんの span。この span の中で出したログ（ハンドラの tracing::error! など）には、
// すべて method・path・request_id が付く。
// path だけを記録するのは、クエリ文字列（?token=... のような値）をログに残さないため。
// ヘッダーは丸ごとは記録しない（記録するなら、上の SetSensitiveRequestHeadersLayer が authorization を伏せる）
fn make_span(request: &Request) -> Span {
    let request_id = request
        .headers()
        .get(X_REQUEST_ID)
        .and_then(|value| value.to_str().ok())
        .unwrap_or("-");
    tracing::info_span!(
        "request",
        method = %request.method(),
        path = request.uri().path(),
        request_id,
    )
}

// API の応答向けの組み合わせ（OWASP REST Security Cheat Sheet）。
// if_not_present: ハンドラが自分で同じヘッダーを付けていたら、上書きしない
fn security_headers(router: Router) -> Router {
    router
        // Content-Type を推測せず、宣言どおりに扱わせる（JSON を HTML として解釈させない）
        .layer(SetResponseHeaderLayer::if_not_present(
            X_CONTENT_TYPE_OPTIONS,
            HeaderValue::from_static("nosniff"),
        ))
        // この応答からは何も読み込ませず、どのページの枠（iframe など）にも入れさせない
        .layer(SetResponseHeaderLayer::if_not_present(
            CONTENT_SECURITY_POLICY,
            HeaderValue::from_static("default-src 'none'; frame-ancestors 'none'"),
        ))
        // 遷移先に Referer（遷移元の URL）を送らせない
        .layer(SetResponseHeaderLayer::if_not_present(
            REFERRER_POLICY,
            HeaderValue::from_static("no-referrer"),
        ))
        // 応答を共有キャッシュにも、ブラウザのキャッシュにも保存させない（トークンを含む応答があるため）
        .layer(SetResponseHeaderLayer::if_not_present(
            CACHE_CONTROL,
            HeaderValue::from_static("no-store"),
        ))
}

// credentials（Cookie など）は許可しない。Bearer トークンは Authorization ヘッダーで送るので不要。
// オリジンは完全一致（スキーム・ホスト・ポートが同じで、末尾の `/` は付けない）
fn cors(config: &Config) -> CorsLayer {
    CorsLayer::new()
        .allow_origin(AllowOrigin::list(config.cors_allowed_origins.clone()))
        .allow_methods([Method::GET, Method::POST, Method::PATCH, Method::DELETE])
        .allow_headers([AUTHORIZATION, CONTENT_TYPE])
        // ブラウザの JavaScript が 429 の待ち時間を読めるようにする
        .expose_headers([RETRY_AFTER])
        // プリフライトの結果をブラウザが 10 分間キャッシュする
        .max_age(Duration::from_secs(600))
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
