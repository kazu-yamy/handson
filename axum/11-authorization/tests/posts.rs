mod common;

use axum::http::StatusCode;
use common::{authed_json, create_post, get, json_request, register_user, send, test_app};
use sqlx::SqlitePool;

#[sqlx::test]
async fn create_then_get_post(pool: SqlitePool) {
    let app = test_app(pool);
    let alice = register_user(&app, "alice").await;

    let (status, created) = send(
        app.clone(),
        authed_json(
            "POST",
            "/posts",
            &alice.token,
            r#"{"title":"  Hello  ","body":"first post"}"#,
        ),
    )
    .await;
    assert_eq!(status, StatusCode::CREATED);
    assert_eq!(created["author_id"], alice.id); // 著者はトークンの持ち主
    assert_eq!(created["title"], "Hello"); // 前後の空白は取り除いて保存する
    assert_eq!(created["like_count"], 0);
    assert!(created["created_at"].as_str().unwrap().ends_with('Z'));
    let id = created["id"].as_i64().unwrap();

    let (status, fetched) = send(app, get(&format!("/posts/{id}"))).await;
    assert_eq!(status, StatusCode::OK);
    assert_eq!(fetched, created);
}

#[sqlx::test]
async fn create_without_a_token_is_unauthorized(pool: SqlitePool) {
    let (status, body) = send(
        test_app(pool),
        json_request("POST", "/posts", None, r#"{"title":"t","body":"b"}"#),
    )
    .await;
    assert_eq!(status, StatusCode::UNAUTHORIZED);
    assert_eq!(body["error"]["code"], "unauthorized");
}

#[sqlx::test]
async fn authentication_runs_before_the_body_is_parsed(pool: SqlitePool) {
    // トークンが無ければ、壊れた JSON でも 400 ではなく 401（AuthUser が Json より前に評価される）
    let (status, _) = send(
        test_app(pool),
        json_request("POST", "/posts", None, r#"{"title":"#),
    )
    .await;
    assert_eq!(status, StatusCode::UNAUTHORIZED);
}

#[sqlx::test]
async fn author_id_in_the_body_is_ignored(pool: SqlitePool) {
    let app = test_app(pool);
    let alice = register_user(&app, "alice").await;
    let bob = register_user(&app, "bob").await;

    // alice のトークンで bob 名義の投稿を試みても、著者は alice になる（なりすましはできない）
    let (status, created) = send(
        app,
        authed_json(
            "POST",
            "/posts",
            &alice.token,
            &format!(r#"{{"author_id":{},"title":"t","body":"b"}}"#, bob.id),
        ),
    )
    .await;
    assert_eq!(status, StatusCode::CREATED);
    assert_eq!(created["author_id"], alice.id);
}

#[sqlx::test]
async fn create_reports_every_invalid_field_at_once(pool: SqlitePool) {
    let app = test_app(pool);
    let alice = register_user(&app, "alice").await;
    let long_body = "x".repeat(5001);

    let (status, body) = send(
        app,
        authed_json(
            "POST",
            "/posts",
            &alice.token,
            &format!(r#"{{"title":"   ","body":"{long_body}"}}"#),
        ),
    )
    .await;
    assert_eq!(status, StatusCode::UNPROCESSABLE_ENTITY);
    assert_eq!(body["error"]["code"], "validation_failed");
    assert!(body["error"]["fields"]["title"].is_array());
    assert!(body["error"]["fields"]["body"].is_array());
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
    let alice = register_user(&app, "alice").await;
    let post = create_post(&app, &alice.token, "before").await;
    let id = post["id"].as_i64().unwrap();

    let (status, updated) = send(
        app,
        authed_json(
            "PATCH",
            &format!("/posts/{id}"),
            &alice.token,
            r#"{"title":"after"}"#,
        ),
    )
    .await;
    assert_eq!(status, StatusCode::OK);
    assert_eq!(updated["title"], "after");
    assert_eq!(updated["body"], post["body"]); // 未指定の body はそのまま
    assert_eq!(updated["author_id"], alice.id);
}

#[sqlx::test]
async fn patch_with_empty_title_is_unprocessable(pool: SqlitePool) {
    let app = test_app(pool);
    let alice = register_user(&app, "alice").await;
    let post = create_post(&app, &alice.token, "before").await;
    let id = post["id"].as_i64().unwrap();

    let (status, body) = send(
        app,
        authed_json(
            "PATCH",
            &format!("/posts/{id}"),
            &alice.token,
            r#"{"title":""}"#,
        ),
    )
    .await;
    assert_eq!(status, StatusCode::UNPROCESSABLE_ENTITY);
    assert!(body["error"]["fields"]["title"].is_array());
}

#[sqlx::test]
async fn patch_unknown_post_is_not_found(pool: SqlitePool) {
    let app = test_app(pool);
    let alice = register_user(&app, "alice").await;
    let (status, _) = send(
        app,
        authed_json("PATCH", "/posts/999", &alice.token, r#"{"title":"x"}"#),
    )
    .await;
    assert_eq!(status, StatusCode::NOT_FOUND);
}

#[sqlx::test]
async fn patch_and_delete_without_a_token_are_unauthorized(pool: SqlitePool) {
    let app = test_app(pool);
    let alice = register_user(&app, "alice").await;
    let post = create_post(&app, &alice.token, "title").await;
    let id = post["id"].as_i64().unwrap();

    let (status, _) = send(
        app.clone(),
        json_request("PATCH", &format!("/posts/{id}"), None, r#"{"title":"x"}"#),
    )
    .await;
    assert_eq!(status, StatusCode::UNAUTHORIZED);
    let (status, _) = send(
        app.clone(),
        json_request("DELETE", &format!("/posts/{id}"), None, ""),
    )
    .await;
    assert_eq!(status, StatusCode::UNAUTHORIZED);

    // 記事は変わっていない
    let (_, fetched) = send(app, get(&format!("/posts/{id}"))).await;
    assert_eq!(fetched, post);
}

#[sqlx::test]
async fn someone_elses_post_cannot_be_patched_and_looks_missing(pool: SqlitePool) {
    let app = test_app(pool);
    let alice = register_user(&app, "alice").await;
    let bob = register_user(&app, "bob").await;
    let post = create_post(&app, &alice.token, "alice's").await;
    let id = post["id"].as_i64().unwrap();

    // bob が alice の記事を PATCH する。403 ではなく 404（存在しない記事と同じ応答）
    let (status, body) = send(
        app.clone(),
        authed_json(
            "PATCH",
            &format!("/posts/{id}"),
            &bob.token,
            r#"{"title":"hacked"}"#,
        ),
    )
    .await;
    assert_eq!(status, StatusCode::NOT_FOUND);
    let (_, missing) = send(
        app.clone(),
        authed_json("PATCH", "/posts/999", &bob.token, r#"{"title":"hacked"}"#),
    )
    .await;
    assert_eq!(body, missing);

    // 記事は変わっていない
    let (_, fetched) = send(app, get(&format!("/posts/{id}"))).await;
    assert_eq!(fetched, post);
}

#[sqlx::test]
async fn someone_elses_post_cannot_be_deleted_and_looks_missing(pool: SqlitePool) {
    let app = test_app(pool);
    let alice = register_user(&app, "alice").await;
    let bob = register_user(&app, "bob").await;
    let post = create_post(&app, &alice.token, "alice's").await;
    let id = post["id"].as_i64().unwrap();

    let (status, body) = send(
        app.clone(),
        authed_json("DELETE", &format!("/posts/{id}"), &bob.token, ""),
    )
    .await;
    assert_eq!(status, StatusCode::NOT_FOUND);
    let (_, missing) = send(
        app.clone(),
        authed_json("DELETE", "/posts/999", &bob.token, ""),
    )
    .await;
    assert_eq!(body, missing);

    // 記事は残っている
    let (status, fetched) = send(app, get(&format!("/posts/{id}"))).await;
    assert_eq!(status, StatusCode::OK);
    assert_eq!(fetched, post);
}

#[sqlx::test]
async fn delete_removes_the_post(pool: SqlitePool) {
    let app = test_app(pool);
    let alice = register_user(&app, "alice").await;
    let post = create_post(&app, &alice.token, "title").await;
    let id = post["id"].as_i64().unwrap();

    let (status, _) = send(
        app.clone(),
        authed_json("DELETE", &format!("/posts/{id}"), &alice.token, ""),
    )
    .await;
    assert_eq!(status, StatusCode::NO_CONTENT);

    let (status, _) = send(app, get(&format!("/posts/{id}"))).await;
    assert_eq!(status, StatusCode::NOT_FOUND);
}

#[sqlx::test]
async fn list_pages_newest_first_with_next_cursor(pool: SqlitePool) {
    let app = test_app(pool);
    let alice = register_user(&app, "alice").await;
    let first = create_post(&app, &alice.token, "first").await;
    let second = create_post(&app, &alice.token, "second").await;
    let third = create_post(&app, &alice.token, "third").await;

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
    let alice = register_user(&app, "alice").await;
    create_post(&app, &alice.token, "a").await;
    create_post(&app, &alice.token, "b").await;

    let (_, page) = send(app, get("/posts?limit=2")).await;
    assert_eq!(page["items"].as_array().unwrap().len(), 2);
    assert_eq!(page["next_cursor"], serde_json::Value::Null);
}

#[sqlx::test]
async fn list_filters_by_author(pool: SqlitePool) {
    let app = test_app(pool);
    let alice = register_user(&app, "alice").await;
    let bob = register_user(&app, "bob").await;
    create_post(&app, &alice.token, "alice 1").await;
    create_post(&app, &bob.token, "bob 1").await;
    create_post(&app, &alice.token, "alice 2").await;

    // 読み取りはトークン無しで呼べる
    let (_, page) = send(app, get(&format!("/posts?author_id={}", alice.id))).await;
    let items = page["items"].as_array().unwrap();
    assert_eq!(items.len(), 2);
    assert!(items.iter().all(|post| post["author_id"] == alice.id));
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
    let alice = register_user(&app, "alice").await;
    let post = create_post(&app, &alice.token, "title").await;
    let id = post["id"].as_i64().unwrap();

    // いいねは認証なしで 2 回押すと like_count は 2
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
    let alice = register_user(&app, "alice").await;
    let post = create_post(&app, &alice.token, "title").await;
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
    let app = test_app(pool.clone());
    let alice = register_user(&app, "alice").await;
    let bob = register_user(&app, "bob").await;
    let alice_post = create_post(&app, &alice.token, "alice's").await;
    create_post(&app, &bob.token, "bob's").await;

    // 利用者の削除 API は無いので、DB を直接操作する
    sqlx::query("DELETE FROM users WHERE id = ?")
        .bind(alice.id)
        .execute(&pool)
        .await
        .unwrap();

    // ON DELETE CASCADE: alice の記事だけが消え、bob の記事は残る
    let (status, _) = send(app.clone(), get(&format!("/posts/{}", alice_post["id"]))).await;
    assert_eq!(status, StatusCode::NOT_FOUND);
    let (_, page) = send(app, get("/posts")).await;
    assert_eq!(page["items"].as_array().unwrap().len(), 1);
}
