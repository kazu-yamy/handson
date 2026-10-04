use app::{
    build_app, config::Config, password, rate_limit::RateLimits, state::AppState, telemetry,
};
use sqlx::sqlite::{SqliteConnectOptions, SqliteJournalMode, SqlitePoolOptions};
use std::net::SocketAddr;
use std::str::FromStr;
use std::sync::Arc;
use std::time::Duration;
use tokio::signal;

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    // .env に書いた RUST_LOG / LOG_FORMAT も効くよう、dotenv の後で初期化する
    dotenvy::dotenv().ok();
    telemetry::init();
    let config = Config::from_env()?;
    tracing::info!(?config, "config loaded");
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
    // 停止の最後にプールを閉じるため、AppState（Router の中に移る）とは別に持っておく。SqlitePool の clone は同じプールを指す
    let pool = state.pool.clone();
    let app = build_app(state);

    let listener = tokio::net::TcpListener::bind(&addr).await?;
    tracing::info!(addr = %listener.local_addr()?, "listening");
    // ConnectInfo（接続元のアドレス）を取り出せるようにして serve する。これが無いと IP 単位の制限が 500 になる
    // with_graceful_shutdown: シグナルを受けたら新しい接続を受け付けず、処理中のリクエストが終わってから serve が戻る
    axum::serve(
        listener,
        app.into_make_service_with_connect_info::<SocketAddr>(),
    )
    .with_graceful_shutdown(shutdown_signal())
    .await?;

    // 処理中のリクエストが無くなってから DB 接続を閉じる。WAL の書き込みを残さずに終わる
    pool.close().await;
    tracing::info!("server stopped");
    Ok(())
}

// Ctrl+C（SIGINT）と、Unix では SIGTERM（docker stop や Kubernetes が送る）のどちらかを待つ
async fn shutdown_signal() {
    let ctrl_c = async {
        signal::ctrl_c()
            .await
            .expect("failed to install Ctrl+C handler");
    };

    #[cfg(unix)]
    let terminate = async {
        signal::unix::signal(signal::unix::SignalKind::terminate())
            .expect("failed to install SIGTERM handler")
            .recv()
            .await;
    };

    // Unix 以外には SIGTERM が無いので、永遠に完了しない future に置き換える
    #[cfg(not(unix))]
    let terminate = std::future::pending::<()>();

    tokio::select! {
        _ = ctrl_c => {},
        _ = terminate => {},
    }
    tracing::info!("shutdown signal received");
}
