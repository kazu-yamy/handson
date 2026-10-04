use std::io::IsTerminal;
use tracing_subscriber::{EnvFilter, fmt, layer::SubscriberExt, util::SubscriberInitExt};

// ログの出力先（subscriber）を作って、プロセスに 1 つだけ登録する。
// 2 回呼ぶと panic するので、main からだけ呼ぶ（テストからは呼ばない）
pub fn init() {
    // RUST_LOG が無い、または書式が壊れているときは info 以上を出す
    let filter = EnvFilter::try_from_default_env().unwrap_or_else(|_| EnvFilter::new("info"));
    let registry = tracing_subscriber::registry().with(filter);

    if std::env::var("LOG_FORMAT").is_ok_and(|value| value == "json") {
        // 1 行 1 JSON。ログ収集の基盤（Cloud Logging や Datadog など）に渡しやすい形
        registry.with(fmt::layer().json()).init();
    } else {
        // 色の escape sequence は、出力先が端末のときだけ付ける（ファイルにリダイレクトしたときに混ざらないように）
        registry
            .with(fmt::layer().with_ansi(std::io::stdout().is_terminal()))
            .init();
    }
}
