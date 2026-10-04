use app::{build_app, config::Config, state::AppState};
use sqlx::SqlitePool;
use std::sync::Arc;

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    dotenvy::dotenv().ok();
    let config = Config::from_env()?;
    println!("config: {config:?}");

    let pool = SqlitePool::connect(&config.database_url).await?;
    sqlx::migrate!().run(&pool).await?;

    let addr = format!("{}:{}", config.host, config.port);
    let state = AppState {
        pool,
        config: Arc::new(config),
    };
    let app = build_app(state);

    let listener = tokio::net::TcpListener::bind(&addr).await?;
    println!("listening on {}", listener.local_addr()?);
    axum::serve(listener, app).await?;
    Ok(())
}
