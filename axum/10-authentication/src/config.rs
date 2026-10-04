use std::env;
use std::fmt;

pub struct Config {
    pub host: String,
    pub port: u16,
    pub database_url: String,
    pub api_key: String,
    pub session_ttl_secs: i64,
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
        let api_key = env::var("API_KEY")
            .ok()
            .filter(|value| !value.is_empty())
            .ok_or(ConfigError::Missing("API_KEY"))?;
        // セッション（トークン）の有効期間。既定は 7 日
        let session_ttl_secs = match env::var("SESSION_TTL_SECS") {
            Ok(value) => value
                .parse()
                .ok()
                .filter(|secs| *secs > 0)
                .ok_or(ConfigError::Invalid("SESSION_TTL_SECS", value))?,
            Err(_) => 604800,
        };

        Ok(Config {
            host,
            port,
            database_url,
            api_key,
            session_ttl_secs,
        })
    }
}

impl fmt::Debug for Config {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.debug_struct("Config")
            .field("host", &self.host)
            .field("port", &self.port)
            .field("database_url", &self.database_url)
            .field("api_key", &"***")
            .field("session_ttl_secs", &self.session_ttl_secs)
            .finish()
    }
}
