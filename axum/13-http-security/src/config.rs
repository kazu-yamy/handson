use axum::http::HeaderValue;
use std::env;
use std::fmt;
use std::time::Duration;

pub struct Config {
    pub host: String,
    pub port: u16,
    pub database_url: String,
    pub session_ttl_secs: i64,
    pub request_timeout: Duration,
    // true のときだけ、IP 単位のレート制限で X-Forwarded-For の右端を信用する
    pub trust_forwarded_for: bool,
    // CORS で許可するオリジン。空なら CORS のレイヤーを掛けない
    pub cors_allowed_origins: Vec<HeaderValue>,
}

pub enum ConfigError {
    Missing(&'static str),
    Invalid(&'static str, String),
}

impl fmt::Display for ConfigError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            ConfigError::Missing(name) => write!(f, "{name} must be set"),
            ConfigError::Invalid(name, value) => write!(f, "{name} is invalid: {value:?}"),
        }
    }
}

// main が Err を返すと Debug 形式で表示されるため、Display と同じ文言にする
impl fmt::Debug for ConfigError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        fmt::Display::fmt(self, f)
    }
}

impl std::error::Error for ConfigError {}

fn parse_origins(value: &str) -> Result<Vec<HeaderValue>, ConfigError> {
    value
        .split(',')
        .map(str::trim)
        .filter(|origin| !origin.is_empty())
        .map(|origin| {
            HeaderValue::from_str(origin)
                .map_err(|_| ConfigError::Invalid("CORS_ALLOWED_ORIGINS", origin.to_string()))
        })
        .collect()
}

impl Config {
    pub fn from_env() -> Result<Config, ConfigError> {
        let host = env::var("APP_HOST").unwrap_or_else(|_| "127.0.0.1".to_string());
        let port = match env::var("APP_PORT") {
            Ok(value) => value
                .parse()
                .map_err(|_| ConfigError::Invalid("APP_PORT", value))?,
            Err(_) => 8080,
        };
        let database_url =
            env::var("DATABASE_URL").unwrap_or_else(|_| "sqlite://app.db".to_string());
        // セッション（トークン）の有効期間。既定は 7 日
        let session_ttl_secs = match env::var("SESSION_TTL_SECS") {
            Ok(value) => value
                .parse()
                .ok()
                .filter(|secs| *secs > 0)
                .ok_or(ConfigError::Invalid("SESSION_TTL_SECS", value))?,
            Err(_) => 604800,
        };

        // 1 つのリクエストの処理にかけてよい時間（秒）。正の整数。既定は 10 秒
        let request_timeout = match env::var("REQUEST_TIMEOUT_SECS") {
            Ok(value) => value
                .parse()
                .ok()
                .filter(|secs| *secs > 0)
                .map(Duration::from_secs)
                .ok_or(ConfigError::Invalid("REQUEST_TIMEOUT_SECS", value))?,
            Err(_) => Duration::from_secs(10),
        };

        // 信用するプロキシの後ろで動かすときだけ true にする。既定は false。true / false 以外は起動時エラー
        let trust_forwarded_for = match env::var("TRUST_FORWARDED_FOR") {
            Ok(value) => match value.as_str() {
                "true" => true,
                "false" => false,
                _ => return Err(ConfigError::Invalid("TRUST_FORWARDED_FOR", value)),
            },
            Err(_) => false,
        };

        // カンマ区切り。前後の空白は取り、空の要素は無視する。空（未設定を含む）なら CORS は無効
        let cors_allowed_origins = match env::var("CORS_ALLOWED_ORIGINS") {
            Ok(value) => parse_origins(&value)?,
            Err(_) => Vec::new(),
        };

        Ok(Config {
            host,
            port,
            database_url,
            session_ttl_secs,
            request_timeout,
            trust_forwarded_for,
            cors_allowed_origins,
        })
    }
}

impl fmt::Debug for Config {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.debug_struct("Config")
            .field("host", &self.host)
            .field("port", &self.port)
            .field("database_url", &self.database_url)
            .field("session_ttl_secs", &self.session_ttl_secs)
            .field("request_timeout", &self.request_timeout)
            .field("trust_forwarded_for", &self.trust_forwarded_for)
            .field("cors_allowed_origins", &self.cors_allowed_origins)
            .finish()
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn origins_are_split_on_commas_and_trimmed() {
        let origins = parse_origins(" http://localhost:3000 ,https://app.example.com,, ").unwrap();
        assert_eq!(
            origins,
            ["http://localhost:3000", "https://app.example.com"]
        );
        assert!(parse_origins("").unwrap().is_empty());
    }

    #[test]
    fn an_origin_that_is_not_a_valid_header_value_is_an_error() {
        assert!(parse_origins("http://localhost:3000\n").is_ok()); // trim が改行を取る
        assert!(parse_origins("http://exa\u{7f}mple.com").is_err());
    }
}
