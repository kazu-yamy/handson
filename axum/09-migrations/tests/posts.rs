mod common;

use axum::http::StatusCode;
use common::{TEST_API_KEY, create_post, create_user, get, json_request, send, test_app};
use sqlx::SqlitePool;

#[sqlx::test]
async fn create_then_get_post(pool: SqlitePool) {
    let app = test_app(pool);
    let author_id = create_user(&app, "alice").await;

    let (status, created) = send(
        app.clone(),
        json_request(
            "POST",
            "/posts",
            Some(TEST_API_KEY),
            &format!(r#"{{"author_id":{author_id},"title":"  Hello  ","body":"first post"}}"#),
        ),
    )
    .await;
    assert_eq!(status, StatusCode::CREATED);
    assert_eq!(created["author_id"], author_id);
    assert_eq!(created["title"], "Hello"); // 前後の空白は取り除いて保存する
    assert_eq!(created["like_count"], 0);
    assert!(created["created_at"].as_str().unwrap().ends_with('Z'));
    let id = created["id"].as_i64().unwrap();

    let (status, fetched) = send(app, get(&format!("/posts/{id}"))).await;
    assert_eq!(status, StatusCode::OK);
    assert_eq!(fetched, created);
}

#[sqlx::test]
async fn create_without_api_key_is_unauthorized(pool: SqlitePool) {
    let (status, body) = send(
        test_app(pool),
        json_request(
            "POST",
            "/posts",
            None,
            r#"{"author_id":1,"title":"t","body":"b"}"#,
        ),
    )
    .await;
    assert_eq!(status, StatusCode::UNAUTHORIZED);
    assert_eq!(body["error"]["code"], "unauthorized");
}

#[sqlx::test]
async fn create_reports_every_invalid_field_at_once(pool: SqlitePool) {
    let app = test_app(pool);
    let author_id = create_user(&app, "alice").await;
    let long_body = "x".repeat(5001);

    let (status, body) = send(
        app,
        json_request(
            "POST",
            "/posts",
            Some(TEST_API_KEY),
            &format!(r#"{{"author_id":{author_id},"title":"   ","body":"{long_body}"}}"#),
        ),
    )
    .await;
    assert_eq!(status, StatusCode::UNPROCESSABLE_ENTITY);
    assert_eq!(body["error"]["code"], "validation_failed");
    assert!(body["error"]["fields"]["title"].is_array());
    assert!(body["error"]["fields"]["body"].is_array());
}

#[sqlx::test]
async fn create_with_unknown_author_is_unprocessable(pool: SqlitePool) {
    let (status, body) = send(
        test_app(pool),
        json_request(
            "POST",
            "/posts",
            Some(TEST_API_KEY),
            r#"{"author_id":999,"title":"t","body":"b"}"#,
        ),
    )
    .await;
    assert_eq!(status, StatusCode::UNPROCESSABLE_ENTITY);
    assert_eq!(
        body["error"]["fields"]["author_id"],
        serde_json::json!(["author not found"])
    );
}

#[sqlx::test]
async fn get_unknown_post_is_not_found(pool: SqlitePool) {
    let (status, body) = send(test_app(pool), get("/posts/999")).await;
    assert_eq!(status, StatusCode::NOT_FOUND);
    assert_eq!(body["error"]["code"], "not_found");
    assert_eq!(body["error"]["message"], "post not found");
}

#[sqlx::test]
async fn patch_changes_only_the_given_fields(pool: SqlitePool) {
    let app = test_app(pool);
    let author_id = create_user(&app, "alice").await;
    let post = create_post(&app, author_id, "before").await;
    let id = post["id"].as_i64().unwrap();

    let (status, updated) = send(
        app,
        json_request(
            "PATCH",
            &format!("/posts/{id}"),
            Some(TEST_API_KEY),
            r#"{"title":"after"}"#,
        ),
    )
    .await;
    assert_eq!(status, StatusCode::OK);
    assert_eq!(updated["title"], "after");
    assert_eq!(updated["body"], post["body"]); // 未指定の body はそのまま
}

#[sqlx::test]
async fn patch_with_empty_title_is_unprocessable(pool: SqlitePool) {
    let app = test_app(pool);
    let author_id = create_user(&app, "alice").await;
    let post = create_post(&app, author_id, "before").await;
    let id = post["id"].as_i64().unwrap();

    let (status, body) = send(
        app,
        json_request(
            "PATCH",
            &format!("/posts/{id}"),
            Some(TEST_API_KEY),
            r#"{"title":""}"#,
        ),
    )
    .await;
    assert_eq!(status, StatusCode::UNPROCESSABLE_ENTITY);
    assert!(body["error"]["fields"]["title"].is_array());
}

#[sqlx::test]
async fn patch_unknown_post_is_not_found(pool: SqlitePool) {
    let (status, _) = send(
        test_app(pool),
        json_request(
            "PATCH",
            "/posts/999",
            Some(TEST_API_KEY),
            r#"{"title":"x"}"#,
        ),
    )
    .await;
    assert_eq!(status, StatusCode::NOT_FOUND);
}

#[sqlx::test]
async fn delete_removes_the_post(pool: SqlitePool) {
    let app = test_app(pool);
    let author_id = create_user(&app, "alice").await;
    let post = create_post(&app, author_id, "title").await;
    let id = post["id"].as_i64().unwrap();

    let (status, _) = send(
        app.clone(),
        json_request("DELETE", &format!("/posts/{id}"), Some(TEST_API_KEY), ""),
    )
    .await;
    assert_eq!(status, StatusCode::NO_CONTENT);

    let (status, _) = send(app, get(&format!("/posts/{id}"))).await;
    assert_eq!(status, StatusCode::NOT_FOUND);
}

#[sqlx::test]
async fn list_pages_newest_first_with_next_cursor(pool: SqlitePool) {
    let app = test_app(pool);
    let author_id = create_user(&app, "alice").await;
    let first = create_post(&app, author_id, "first").await;
    let second = create_post(&app, author_id, "second").await;
    let third = create_post(&app, author_id, "third").await;

    // 1 ページ目: 新しい順に 2 件、続きがあるので next_cursor は最後の id
    let (status, page1) = send(app.clone(), get("/posts?limit=2")).await;
    assert_eq!(status, StatusCode::OK);
    let ids: Vec<_> = page1["items"]
        .as_array()
        .unwrap()
        .iter()
        .map(|post| post["id"].clone())
        .collect();
    assert_eq!(ids, vec![third["id"].clone(), second["id"].clone()]);
    assert_eq!(page1["next_cursor"], second["id"]);

    // 2 ページ目: next_cursor をそのまま渡すと残り 1 件で、next_cursor は null
    let cursor = page1["next_cursor"].as_i64().unwrap();
    let (_, page2) = send(app, get(&format!("/posts?limit=2&cursor={cursor}"))).await;
    assert_eq!(page2["items"].as_array().unwrap().len(), 1);
    assert_eq!(page2["items"][0]["id"], first["id"]);
    assert_eq!(page2["next_cursor"], serde_json::Value::Null);
}

#[sqlx::test]
async fn list_with_exactly_limit_items_has_no_next_cursor(pool: SqlitePool) {
    let app = test_app(pool);
    let author_id = create_user(&app, "alice").await;
    create_post(&app, author_id, "a").await;
    create_post(&app, author_id, "b").await;

    let (_, page) = send(app, get("/posts?limit=2")).await;
    assert_eq!(page["items"].as_array().unwrap().len(), 2);
    assert_eq!(page["next_cursor"], serde_json::Value::Null);
}

#[sqlx::test]
async fn list_filters_by_author(pool: SqlitePool) {
    let app = test_app(pool);
    let alice = create_user(&app, "alice").await;
    let bob = create_user(&app, "bob").await;
    create_post(&app, alice, "alice 1").await;
    create_post(&app, bob, "bob 1").await;
    create_post(&app, alice, "alice 2").await;

    let (_, page) = send(app, get(&format!("/posts?author_id={alice}"))).await;
    let items = page["items"].as_array().unwrap();
    assert_eq!(items.len(), 2);
    assert!(items.iter().all(|post| post["author_id"] == alice));
}

#[sqlx::test]
async fn list_with_limit_out_of_range_is_unprocessable(pool: SqlitePool) {
    let app = test_app(pool);
    for uri in ["/posts?limit=0", "/posts?limit=101", "/posts?limit=-1"] {
        let (status, body) = send(app.clone(), get(uri)).await;
        assert_eq!(status, StatusCode::UNPROCESSABLE_ENTITY, "{uri}");
        assert!(body["error"]["fields"]["limit"].is_array(), "{uri}");
    }
}

#[sqlx::test]
async fn list_with_malformed_query_is_bad_request(pool: SqlitePool) {
    let app = test_app(pool);
    for uri in [
        "/posts?limit=abc",
        "/posts?cursor=abc",
        "/posts?author_id=x",
    ] {
        let (status, body) = send(app.clone(), get(uri)).await;
        assert_eq!(status, StatusCode::BAD_REQUEST, "{uri}");
        assert_eq!(body["error"]["code"], "bad_request", "{uri}");
    }
}

#[sqlx::test]
async fn like_increments_the_count(pool: SqlitePool) {
    let app = test_app(pool);
    let author_id = create_user(&app, "alice").await;
    let post = create_post(&app, author_id, "title").await;
    let id = post["id"].as_i64().unwrap();

    // 認証なしで 2 回押すと like_count は 2
    for expected in [1, 2] {
        let (status, liked) = send(
            app.clone(),
            json_request("POST", &format!("/posts/{id}/like"), None, ""),
        )
        .await;
        assert_eq!(status, StatusCode::OK);
        assert_eq!(
            liked,
            serde_json::json!({ "id": id, "like_count": expected })
        );
    }
}

#[sqlx::test]
async fn like_unknown_post_is_not_found(pool: SqlitePool) {
    let (status, _) = send(
        test_app(pool),
        json_request("POST", "/posts/999/like", None, ""),
    )
    .await;
    assert_eq!(status, StatusCode::NOT_FOUND);
}

#[sqlx::test]
async fn concurrent_likes_are_not_lost(pool: SqlitePool) {
    let app = test_app(pool);
    let author_id = create_user(&app, "alice").await;
    let post = create_post(&app, author_id, "title").await;
    let id = post["id"].as_i64().unwrap();

    // 50 件を同時に押しても、原子的な UPDATE なので 1 件も失われない
    let mut tasks = Vec::new();
    for _ in 0..50 {
        let app = app.clone();
        tasks.push(tokio::spawn(async move {
            send(
                app,
                json_request("POST", &format!("/posts/{id}/like"), None, ""),
            )
            .await
        }));
    }
    for task in tasks {
        let (status, _) = task.await.unwrap();
        assert_eq!(status, StatusCode::OK);
    }

    let (_, fetched) = send(app, get(&format!("/posts/{id}"))).await;
    assert_eq!(fetched["like_count"], 50);
}

#[sqlx::test]
async fn deleting_a_user_deletes_their_posts(pool: SqlitePool) {
    let app = test_app(pool);
    let alice = create_user(&app, "alice").await;
    let bob = create_user(&app, "bob").await;
    let alice_post = create_post(&app, alice, "alice's").await;
    create_post(&app, bob, "bob's").await;

    let (status, _) = send(
        app.clone(),
        json_request("DELETE", &format!("/users/{alice}"), Some(TEST_API_KEY), ""),
    )
    .await;
    assert_eq!(status, StatusCode::NO_CONTENT);

    // ON DELETE CASCADE: alice の記事だけが消え、bob の記事は残る
    let (status, _) = send(app.clone(), get(&format!("/posts/{}", alice_post["id"]))).await;
    assert_eq!(status, StatusCode::NOT_FOUND);
    let (_, page) = send(app, get("/posts")).await;
    assert_eq!(page["items"].as_array().unwrap().len(), 1);
}
