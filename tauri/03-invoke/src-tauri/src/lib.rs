use serde::{Deserialize, Serialize};

#[tauri::command]
fn greet(user_name: &str) -> String {
    format!("こんにちは、{user_name} さん！ Rust から呼び出されました。")
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
struct Todo {
    id: u32,
    title: String,
    done: bool,
    // rename_all = "camelCase" が無いと JSON のキーは created_at のままになり、
    // フロントの todo.createdAt は undefined になる。
    created_at: u64,
}

/// Todo の一覧と次に割り振る id を保持するアプリ状態。
/// `tauri::Builder::manage()` で登録し、各コマンドは `tauri::State<'_, AppState>` で受け取る。
struct AppState {
    todos: std::sync::Mutex<Vec<Todo>>,
    next_id: std::sync::Mutex<u32>,
}

impl AppState {
    fn new() -> Self {
        Self {
            todos: std::sync::Mutex::new(Vec::new()),
            next_id: std::sync::Mutex::new(1),
        }
    }
}

/// アプリ独自のエラー型。`thiserror` でメッセージを定義し、
/// `impl serde::Serialize` でフロントエンドに文字列として返す。
#[derive(Debug, thiserror::Error)]
enum AppError {
    #[error("タイトルを入力してください")]
    EmptyTitle,
    #[error("id {0} の Todo が見つかりません")]
    NotFound(u32),
    #[error("イベント送信に失敗しました: {0}")]
    Emit(String),
}

impl Serialize for AppError {
    fn serialize<S>(&self, serializer: S) -> Result<S::Ok, S::Error>
    where
        S: serde::Serializer,
    {
        serializer.serialize_str(&self.to_string())
    }
}

// --- ここから下は Mutex や tauri::State から切り離した純粋関数。
// ロジック自体は `&mut Vec<Todo>` 等の素のデータ構造だけを操作するため、
// Tauri のモックアプリを使わないテスト（cargo test の高速なユニットテスト）がそのまま書ける。

fn add_todo_pure(
    todos: &mut Vec<Todo>,
    next_id: &mut u32,
    title: String,
) -> Result<Todo, AppError> {
    let title = title.trim().to_string();
    if title.is_empty() {
        return Err(AppError::EmptyTitle);
    }
    let id = *next_id;
    *next_id += 1;
    let todo = Todo {
        id,
        title,
        done: false,
        created_at: current_unix_time(),
    };
    todos.push(todo.clone());
    Ok(todo)
}

fn toggle_todo_pure(todos: &mut [Todo], id: u32) -> Result<Todo, AppError> {
    let todo = todos
        .iter_mut()
        .find(|t| t.id == id)
        .ok_or(AppError::NotFound(id))?;
    todo.done = !todo.done;
    Ok(todo.clone())
}

fn delete_todo_pure(todos: &mut Vec<Todo>, id: u32) -> Result<(), AppError> {
    let len_before = todos.len();
    todos.retain(|t| t.id != id);
    if todos.len() == len_before {
        Err(AppError::NotFound(id))
    } else {
        Ok(())
    }
}

#[tauri::command]
fn add_todo(title: String, state: tauri::State<'_, AppState>) -> Result<Todo, AppError> {
    let mut next_id = state.next_id.lock().unwrap();
    let mut todos = state.todos.lock().unwrap();
    add_todo_pure(&mut todos, &mut next_id, title)
}

#[tauri::command]
fn list_todos(state: tauri::State<'_, AppState>) -> Vec<Todo> {
    state.todos.lock().unwrap().clone()
}

#[tauri::command]
fn toggle_todo(id: u32, state: tauri::State<'_, AppState>) -> Result<Todo, AppError> {
    let mut todos = state.todos.lock().unwrap();
    toggle_todo_pure(&mut todos, id)
}

#[tauri::command]
fn delete_todo(id: u32, state: tauri::State<'_, AppState>) -> Result<(), AppError> {
    let mut todos = state.todos.lock().unwrap();
    delete_todo_pure(&mut todos, id)
}

/// 進捗を `progress` イベントで通知しながら時間のかかる処理を模した非同期コマンド。
#[tauri::command]
async fn slow_task<R: tauri::Runtime>(app: tauri::AppHandle<R>) -> Result<(), AppError> {
    use tauri::Emitter;

    for percent in [0u8, 25, 50, 75, 100] {
        tokio::time::sleep(std::time::Duration::from_millis(200)).await;
        app.emit("progress", percent)
            .map_err(|e| AppError::Emit(e.to_string()))?;
    }
    Ok(())
}

fn current_unix_time() -> u64 {
    std::time::SystemTime::now()
        .duration_since(std::time::UNIX_EPOCH)
        .expect("system clock before unix epoch")
        .as_secs()
}

#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
    tauri::Builder::default()
        .setup(|app| {
            if cfg!(debug_assertions) {
                app.handle().plugin(
                    tauri_plugin_log::Builder::default()
                        .level(log::LevelFilter::Info)
                        .build(),
                )?;
            }
            Ok(())
        })
        .manage(AppState::new())
        .invoke_handler(tauri::generate_handler![
            greet,
            add_todo,
            list_todos,
            toggle_todo,
            delete_todo,
            slow_task
        ])
        .run(tauri::generate_context!())
        .expect("error while running tauri application");
}

#[cfg(test)]
mod tests {
    use super::*;
    use tauri::ipc::{CallbackFn, InvokeBody};
    use tauri::test::{get_ipc_response, mock_builder, mock_context, noop_assets, INVOKE_KEY};
    use tauri::webview::InvokeRequest;

    fn make_app() -> tauri::App<tauri::test::MockRuntime> {
        mock_builder()
            .invoke_handler(tauri::generate_handler![greet])
            .build(mock_context(noop_assets()))
            .expect("failed to build mock app")
    }

    fn make_full_app() -> tauri::App<tauri::test::MockRuntime> {
        mock_builder()
            .manage(AppState::new())
            .invoke_handler(tauri::generate_handler![
                greet,
                add_todo,
                list_todos,
                toggle_todo,
                delete_todo,
                slow_task
            ])
            .build(mock_context(noop_assets()))
            .expect("failed to build mock app")
    }

    fn invoke_request(cmd: &str, body: serde_json::Value) -> InvokeRequest {
        InvokeRequest {
            cmd: cmd.into(),
            callback: CallbackFn(0),
            error: CallbackFn(1),
            url: "tauri://localhost".parse().unwrap(),
            body: InvokeBody::Json(body),
            headers: Default::default(),
            invoke_key: INVOKE_KEY.to_string(),
        }
    }

    #[test]
    fn greet_accepts_camel_case_argument_key() {
        let app = make_app();
        let webview = tauri::WebviewWindowBuilder::new(&app, "main", Default::default())
            .build()
            .unwrap();

        // コマンド引数のキーは camelCase（userName）に変換される。
        let res = get_ipc_response(
            &webview,
            invoke_request("greet", serde_json::json!({ "userName": "World" })),
        );
        assert_eq!(
            res.unwrap().deserialize::<String>().unwrap(),
            "こんにちは、World さん！ Rust から呼び出されました。"
        );
    }

    #[test]
    fn greet_rejects_snake_case_argument_key() {
        let app = make_app();
        let webview = tauri::WebviewWindowBuilder::new(&app, "main", Default::default())
            .build()
            .unwrap();

        // Rust 側の引数名 (user_name) のまま snake_case で渡すと見つからずエラーになる。
        let res = get_ipc_response(
            &webview,
            invoke_request("greet", serde_json::json!({ "user_name": "World" })),
        );
        assert!(res.is_err(), "snake_case キーは受理されないはず: {res:?}");
    }

    #[test]
    fn todo_serializes_created_at_as_camel_case() {
        let todo = Todo {
            id: 1,
            title: "テスト".into(),
            done: false,
            created_at: 0,
        };
        let value = serde_json::to_value(&todo).unwrap();
        assert!(
            value.get("createdAt").is_some(),
            "rename_all = \"camelCase\" により createdAt キーになるはず: {value:?}"
        );
        assert!(
            value.get("created_at").is_none(),
            "snake_case の created_at キーは残らないはず: {value:?}"
        );
    }

    /// rename_all を外した場合の比較用の構造体。
    /// これを使うと JSON のキーは created_at のままになり、
    /// フロントエンドで todo.createdAt を読むと undefined になってしまう。
    #[derive(Serialize)]
    struct TodoWithoutRename {
        created_at: u64,
    }

    #[test]
    fn without_rename_all_key_stays_snake_case() {
        let value = serde_json::to_value(TodoWithoutRename { created_at: 0 }).unwrap();
        assert!(
            value.get("created_at").is_some(),
            "rename_all なしでは created_at のまま: {value:?}"
        );
        assert!(
            value.get("createdAt").is_none(),
            "rename_all なしでは createdAt キーは生成されない（フロントでは undefined になる）: {value:?}"
        );
    }

    #[test]
    fn add_list_toggle_delete_todo_via_state() {
        let app = make_full_app();
        let webview = tauri::WebviewWindowBuilder::new(&app, "main", Default::default())
            .build()
            .unwrap();

        // 追加: 2 件追加すると id が 1, 2 と振られる。
        let todo1 = get_ipc_response(
            &webview,
            invoke_request("add_todo", serde_json::json!({ "title": "牛乳を買う" })),
        )
        .unwrap()
        .deserialize::<Todo>()
        .unwrap();
        assert_eq!(todo1.id, 1);
        assert_eq!(todo1.title, "牛乳を買う");
        assert!(!todo1.done);

        let todo2 = get_ipc_response(
            &webview,
            invoke_request("add_todo", serde_json::json!({ "title": "本を返す" })),
        )
        .unwrap()
        .deserialize::<Todo>()
        .unwrap();
        assert_eq!(todo2.id, 2);

        // 一覧: 状態が Mutex に保持され、2 件とも取得できる。
        let list = get_ipc_response(
            &webview,
            invoke_request("list_todos", serde_json::json!({})),
        )
        .unwrap()
        .deserialize::<Vec<Todo>>()
        .unwrap();
        assert_eq!(list.len(), 2);

        // 完了切替: id=1 の done が true になる。
        let toggled = get_ipc_response(
            &webview,
            invoke_request("toggle_todo", serde_json::json!({ "id": 1 })),
        )
        .unwrap()
        .deserialize::<Todo>()
        .unwrap();
        assert!(toggled.done);

        // 削除: id=1 を消すと一覧は 1 件になる。
        get_ipc_response(
            &webview,
            invoke_request("delete_todo", serde_json::json!({ "id": 1 })),
        )
        .unwrap();
        let list_after_delete = get_ipc_response(
            &webview,
            invoke_request("list_todos", serde_json::json!({})),
        )
        .unwrap()
        .deserialize::<Vec<Todo>>()
        .unwrap();
        assert_eq!(list_after_delete.len(), 1);
        assert_eq!(list_after_delete[0].id, 2);
    }

    #[test]
    fn add_todo_rejects_empty_title() {
        let app = make_full_app();
        let webview = tauri::WebviewWindowBuilder::new(&app, "main", Default::default())
            .build()
            .unwrap();

        let res = get_ipc_response(
            &webview,
            invoke_request("add_todo", serde_json::json!({ "title": "   " })),
        );
        let err = res.expect_err("空タイトルはエラーになるはず");
        assert_eq!(err, serde_json::json!("タイトルを入力してください"));
    }

    #[test]
    fn toggle_and_delete_unknown_id_return_error() {
        let app = make_full_app();
        let webview = tauri::WebviewWindowBuilder::new(&app, "main", Default::default())
            .build()
            .unwrap();

        let toggle_err = get_ipc_response(
            &webview,
            invoke_request("toggle_todo", serde_json::json!({ "id": 999 })),
        )
        .expect_err("存在しない id はエラーになるはず");
        assert_eq!(
            toggle_err,
            serde_json::json!("id 999 の Todo が見つかりません")
        );

        let delete_err = get_ipc_response(
            &webview,
            invoke_request("delete_todo", serde_json::json!({ "id": 999 })),
        )
        .expect_err("存在しない id はエラーになるはず");
        assert_eq!(
            delete_err,
            serde_json::json!("id 999 の Todo が見つかりません")
        );
    }

    #[test]
    fn slow_task_emits_progress_events() {
        use std::sync::{Arc, Mutex as StdMutex};
        use tauri::Listener;

        let app = make_full_app();
        let webview = tauri::WebviewWindowBuilder::new(&app, "main", Default::default())
            .build()
            .unwrap();

        let received: Arc<StdMutex<Vec<u8>>> = Arc::new(StdMutex::new(Vec::new()));
        let received_clone = received.clone();
        app.listen_any("progress", move |event| {
            let payload: u8 = serde_json::from_str(event.payload()).unwrap();
            received_clone.lock().unwrap().push(payload);
        });

        let res = get_ipc_response(&webview, invoke_request("slow_task", serde_json::json!({})));
        assert!(res.is_ok(), "slow_task は成功するはず: {res:?}");

        let payloads = received.lock().unwrap().clone();
        assert_eq!(payloads, vec![0, 25, 50, 75, 100]);
    }

    // --- ここから Mutex や Tauri のモックアプリを使わない、純粋関数だけのユニットテスト。

    #[test]
    fn add_todo_pure_assigns_incrementing_ids() {
        let mut todos = Vec::new();
        let mut next_id = 1u32;

        let todo1 = add_todo_pure(&mut todos, &mut next_id, "牛乳を買う".into()).unwrap();
        let todo2 = add_todo_pure(&mut todos, &mut next_id, "本を返す".into()).unwrap();

        assert_eq!(todo1.id, 1);
        assert_eq!(todo2.id, 2);
        assert_eq!(next_id, 3);
        assert_eq!(todos.len(), 2);
        assert!(!todo1.done);
    }

    #[test]
    fn add_todo_pure_trims_and_rejects_blank_title() {
        let mut todos = Vec::new();
        let mut next_id = 1u32;

        // 前後の空白は trim される。
        let todo = add_todo_pure(&mut todos, &mut next_id, "  牛乳を買う  ".into()).unwrap();
        assert_eq!(todo.title, "牛乳を買う");

        // 空白だけのタイトルは拒否される。
        let err = add_todo_pure(&mut todos, &mut next_id, "   ".into()).unwrap_err();
        assert!(matches!(err, AppError::EmptyTitle));
    }

    #[test]
    fn toggle_todo_pure_flips_done_and_errors_on_missing_id() {
        let mut todos = vec![Todo {
            id: 1,
            title: "牛乳を買う".into(),
            done: false,
            created_at: 0,
        }];

        let toggled = toggle_todo_pure(&mut todos, 1).unwrap();
        assert!(toggled.done);
        assert!(todos[0].done);

        // 再度切り替えると false に戻る。
        let toggled_again = toggle_todo_pure(&mut todos, 1).unwrap();
        assert!(!toggled_again.done);

        let err = toggle_todo_pure(&mut todos, 999).unwrap_err();
        assert!(matches!(err, AppError::NotFound(999)));
    }

    #[test]
    fn delete_todo_pure_removes_item_and_errors_on_missing_id() {
        let mut todos = vec![
            Todo {
                id: 1,
                title: "牛乳を買う".into(),
                done: false,
                created_at: 0,
            },
            Todo {
                id: 2,
                title: "本を返す".into(),
                done: false,
                created_at: 0,
            },
        ];

        delete_todo_pure(&mut todos, 1).unwrap();
        assert_eq!(todos.len(), 1);
        assert_eq!(todos[0].id, 2);

        let err = delete_todo_pure(&mut todos, 1).unwrap_err();
        assert!(matches!(err, AppError::NotFound(1)));
    }
}
