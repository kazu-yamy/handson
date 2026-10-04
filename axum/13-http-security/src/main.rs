use app::{build_app, config::Config, password, rate_limit::RateLimits, state::AppState};
use sqlx::sqlite::{SqliteConnectOptions, SqliteJournalMode, SqlitePoolOptions};
use std::net::SocketAddr;
use std::str::FromStr;
use std::sync::Arc;
use std::time::Duration;

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    dotenvy::dotenv().ok();
    let config = Config::from_env()?;
    println!("config: {config:?}");
    password::warm_up();

    let options = SqliteConnectOptions::from_str(&config.database_url)?
        .create_if_missing(true)
        .foreign_keys(true)
        .journal_mode(SqliteJournalMode::Wal)
        .busy_timeout(Duration::from_secs(5));
    let pool = SqlitePoolOptions::new()
        .max_connections(5)
        .connect_with(options)
        .await?;
    sqlx::migrate!().run(&pool).await?;

    let addr = format!("{}:{}", config.host, config.port);
    let state = AppState {
        pool,
        config: Arc::new(config),
        limits: Arc::new(RateLimits::new()),
    };
    // レート制限のキーが増え続けないよう、60 秒ごとに回復しきったキーを捨てる
    let limits = state.limits.clone();
    tokio::spawn(async move {
        let mut ticker = tokio::time::interval(Duration::from_secs(60));
        loop {
            ticker.tick().await;
            limits.sweep();
        }
    });
    let app = build_app(state);

    let listener = tokio::net::TcpListener::bind(&addr).await?;
    println!("listening on {}", listener.local_addr()?);
    // ConnectInfo（接続元のアドレス）を取り出せるようにして serve する。これが無いと IP 単位の制限が 500 になる
    axum::serve(
        listener,
        app.into_make_service_with_connect_info::<SocketAddr>(),
    )
    .await?;
    Ok(())
}
