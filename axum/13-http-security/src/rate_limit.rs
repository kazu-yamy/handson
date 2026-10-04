use axum::{
    extract::{ConnectInfo, Request, State},
    http::HeaderMap,
    middleware::Next,
    response::Response,
};
use governor::{DefaultKeyedRateLimiter, Quota, clock::Clock};
use std::hash::Hash;
use std::net::{IpAddr, SocketAddr};
use std::num::NonZeroU32;
use std::time::Duration;

use crate::error::AppError;
use crate::state::AppState;

// 制限値は API 契約 4.6 のとおり。コードの定数で、環境変数では変えない。
// Quota::per_minute(n) は「バースト n 回、60 / n 秒ごとに 1 回分回復」（固定窓ではなく GCRA）
const LOGIN_BY_EMAIL_PER_MINUTE: u32 = 5;
const AUTH_BY_IP_PER_MINUTE: u32 = 10;
const LIKE_BY_IP_PER_MINUTE: u32 = 30;

pub struct RateLimits {
    // POST /auth/login。キーは正規化した email（trim + 小文字化）
    pub login_by_email: DefaultKeyedRateLimiter<String>,
    // POST /auth/register と POST /auth/login で共有する。キーは接続元 IP
    pub auth_by_ip: DefaultKeyedRateLimiter<IpAddr>,
    // POST /posts/{id}/like。キーは接続元 IP
    pub like_by_ip: DefaultKeyedRateLimiter<IpAddr>,
}

impl RateLimits {
    pub fn new() -> RateLimits {
        RateLimits::with_quotas(
            Quota::per_minute(NonZeroU32::new(LOGIN_BY_EMAIL_PER_MINUTE).unwrap()),
            Quota::per_minute(NonZeroU32::new(AUTH_BY_IP_PER_MINUTE).unwrap()),
            Quota::per_minute(NonZeroU32::new(LIKE_BY_IP_PER_MINUTE).unwrap()),
        )
    }

    // テストで「時間がたてば回復する」ことを短い時間で確かめるために、上限を差し替えられるようにしておく
    pub fn with_quotas(login_by_email: Quota, auth_by_ip: Quota, like_by_ip: Quota) -> RateLimits {
        RateLimits {
            login_by_email: DefaultKeyedRateLimiter::keyed(login_by_email),
            auth_by_ip: DefaultKeyedRateLimiter::keyed(auth_by_ip),
            like_by_ip: DefaultKeyedRateLimiter::keyed(like_by_ip),
        }
    }
}

impl RateLimits {
    // 回復しきった（＝もう制限に関係しない）キーを捨てる。
    // これをしないと、攻撃者が email や IP を変えて送るたびにキーが増え続け、メモリを食う
    pub fn sweep(&self) {
        self.login_by_email.retain_recent();
        self.login_by_email.shrink_to_fit();
        self.auth_by_ip.retain_recent();
        self.auth_by_ip.shrink_to_fit();
        self.like_by_ip.retain_recent();
        self.like_by_ip.shrink_to_fit();
    }
}

impl Default for RateLimits {
    fn default() -> Self {
        RateLimits::new()
    }
}

// 1 回分の枠を使う。使い切っていたら、次に通るまでの秒数を持つ 429 を返す
pub fn check<K: Hash + Eq + Clone>(
    limiter: &DefaultKeyedRateLimiter<K>,
    key: &K,
) -> Result<(), AppError> {
    limiter.check_key(key).map_err(|not_until| {
        let wait = not_until.wait_time_from(limiter.clock().now());
        AppError::RateLimited {
            retry_after_secs: retry_after_secs(wait),
        }
    })
}

// Retry-After は整数の秒。切り上げて、最小は 1（0 にすると「すぐ再送してよい」と読めてしまう）
pub fn retry_after_secs(wait: Duration) -> u64 {
    let secs = wait.as_secs() + u64::from(wait.subsec_nanos() > 0);
    secs.max(1)
}

// IP 単位の制限に使う、接続元の IP。
// trust_forwarded_for が false（既定）のときは、ヘッダーを見ずに常にソケットの接続元を使う。
// true のときだけ、X-Forwarded-For の「一番右」の値を使う。左の値は送信元が自由に書けるが、
// 右端は信用するプロキシが最後に付け足した値だから。読めない値のときは接続元に戻す
pub fn client_ip(headers: &HeaderMap, peer: SocketAddr, trust_forwarded_for: bool) -> IpAddr {
    if trust_forwarded_for
        && let Some(ip) = headers
            .get_all("x-forwarded-for")
            .iter()
            .next_back()
            .and_then(|value| value.to_str().ok())
            .and_then(|value| value.rsplit(',').next())
            .and_then(|value| value.trim().parse::<IpAddr>().ok())
    {
        return ip;
    }
    peer.ip()
}

// POST /auth/register と POST /auth/login に掛ける（2 つで 1 つの枠を共有する）
pub async fn limit_auth_by_ip(
    State(state): State<AppState>,
    ConnectInfo(peer): ConnectInfo<SocketAddr>,
    request: Request,
    next: Next,
) -> Result<Response, AppError> {
    let ip = client_ip(request.headers(), peer, state.config.trust_forwarded_for);
    check(&state.limits.auth_by_ip, &ip)?;
    Ok(next.run(request).await)
}

// POST /posts/{id}/like に掛ける
pub async fn limit_like_by_ip(
    State(state): State<AppState>,
    ConnectInfo(peer): ConnectInfo<SocketAddr>,
    request: Request,
    next: Next,
) -> Result<Response, AppError> {
    let ip = client_ip(request.headers(), peer, state.config.trust_forwarded_for);
    check(&state.limits.like_by_ip, &ip)?;
    Ok(next.run(request).await)
}

#[cfg(test)]
mod tests {
    use super::*;
    use axum::http::HeaderValue;

    #[tokio::test]
    async fn sweep_drops_keys_that_have_fully_recovered() {
        // 回復が 200 ミリ秒の limiter に 1000 件の email を通す。キーは 1000 件たまる
        let quick = Quota::with_period(Duration::from_millis(200)).unwrap();
        let limits = RateLimits::with_quotas(quick, quick, quick);
        for n in 0..1000 {
            check(&limits.login_by_email, &format!("user{n}@example.com")).unwrap();
        }
        assert_eq!(limits.login_by_email.len(), 1000);

        // まだ回復していないうちは捨てられない
        limits.sweep();
        assert_eq!(limits.login_by_email.len(), 1000);

        // 回復しきった後なら全部捨てられ、掃除した後も同じ email をまた数えられる
        tokio::time::sleep(Duration::from_millis(400)).await;
        limits.sweep();
        assert_eq!(limits.login_by_email.len(), 0);
        assert!(check(&limits.login_by_email, &"user0@example.com".to_string()).is_ok());
    }

    fn peer() -> SocketAddr {
        "192.0.2.10:40000".parse().unwrap()
    }

    fn headers(values: &[&'static str]) -> HeaderMap {
        let mut headers = HeaderMap::new();
        for value in values {
            headers.append("x-forwarded-for", HeaderValue::from_static(value));
        }
        headers
    }

    #[test]
    fn forwarded_for_is_ignored_unless_trusted() {
        let headers = headers(&["203.0.113.1"]);
        assert_eq!(client_ip(&headers, peer(), false), peer().ip());
    }

    #[test]
    fn trusted_forwarded_for_uses_the_rightmost_value() {
        // 左端（198.51.100.99）は送信元が自由に書ける。右端だけが、信用するプロキシが付け足した値
        let headers = headers(&["198.51.100.99, 203.0.113.1"]);
        let expected: IpAddr = "203.0.113.1".parse().unwrap();
        assert_eq!(client_ip(&headers, peer(), true), expected);
    }

    #[test]
    fn trusted_forwarded_for_uses_the_last_header_line() {
        let headers = headers(&["198.51.100.99", "203.0.113.1"]);
        let expected: IpAddr = "203.0.113.1".parse().unwrap();
        assert_eq!(client_ip(&headers, peer(), true), expected);
    }

    #[test]
    fn broken_or_missing_forwarded_for_falls_back_to_the_peer() {
        assert_eq!(
            client_ip(&headers(&["not-an-ip"]), peer(), true),
            peer().ip()
        );
        assert_eq!(
            client_ip(&headers(&["203.0.113.1, "]), peer(), true),
            peer().ip()
        );
        assert_eq!(client_ip(&HeaderMap::new(), peer(), true), peer().ip());
    }
}
