use sqlx::SqlitePool;
use std::sync::Arc;

use crate::config::Config;
use crate::rate_limit::RateLimits;

#[derive(Clone)]
pub struct AppState {
    pub pool: SqlitePool,
    pub config: Arc<Config>,
    // レート制限の状態。プロセスのメモリにあり、Router を複製しても同じものを共有する
    pub limits: Arc<RateLimits>,
}
