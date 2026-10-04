mod common;

use app::layers;
use app::password::{HASH_SLOTS, MAX_CONCURRENT_HASHES, hash_password_blocking};
use axum::{Router, http::StatusCode, routing::get};
use common::{get as get_request, send_raw, test_config};
use std::time::{Duration, Instant};

// HASH_SLOTS はプロセス全体で 1 つなので、この 1 つのテストの中で順番に確かめる
// （同じファイルの別のテストと並列に走ると、permit の数が揺れる）
#[tokio::test]
async fn concurrent_hashes_are_limited_and_slots_are_always_returned() {
    // 1. 上限より多い 8 個を同時に呼んでも、全部成功する（待たされるだけ）。終わったら permit は全部戻る
    let tasks: Vec<_> = (0..8)
        .map(|_| tokio::spawn(hash_password_blocking("correct horse battery".to_string())))
        .collect();
    for task in tasks {
        assert!(task.await.unwrap().is_ok());
    }
    assert_eq!(HASH_SLOTS.available_permits(), MAX_CONCURRENT_HASHES);

    // 2. 順番待ちもタイムアウトに含まれる。ハッシュだけをするハンドラに 100ms のタイムアウトを掛け、
    //    40 件を同時に送る（4 個ずつ処理するので、後ろの方は 100ms に間に合わず 408）
    let mut config = test_config();
    config.request_timeout = Duration::from_millis(100);
    let router = Router::new().route(
        "/hash",
        get(|| async {
            hash_password_blocking("correct horse battery".to_string())
                .await
                .map(|_| "done")
        }),
    );
    let app = layers::apply(router, &config);
    let start = Instant::now();
    let tasks: Vec<_> = (0..40)
        .map(|_| tokio::spawn(send_raw(app.clone(), get_request("/hash"))))
        .collect();
    let mut ok = 0;
    let mut timed_out = 0;
    for task in tasks {
        let (status, _, _) = task.await.unwrap();
        match status {
            StatusCode::OK => ok += 1,
            StatusCode::REQUEST_TIMEOUT => timed_out += 1,
            other => panic!("unexpected status {other}"),
        }
    }
    println!(
        "200: {ok}, 408: {timed_out}, elapsed: {:?}",
        start.elapsed()
    );
    assert!(ok >= 1 && timed_out >= 1, "200: {ok}, 408: {timed_out}");

    // 3. タイムアウトで future が捨てられても、計算中のスレッドが permit を持ち続け、終わったら返す。
    //    計算が全部終わるのを待てば、permit は元の数に戻る
    for _ in 0..100 {
        if HASH_SLOTS.available_permits() == MAX_CONCURRENT_HASHES {
            return;
        }
        tokio::time::sleep(Duration::from_millis(50)).await;
    }
    panic!(
        "permits were not returned: {}",
        HASH_SLOTS.available_permits()
    );
}
