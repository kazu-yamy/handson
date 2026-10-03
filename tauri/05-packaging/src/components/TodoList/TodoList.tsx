"use client";

import { useEffect, useState } from "react";
import { appDataDir } from "@tauri-apps/api/path";
import {
  addTodo,
  deleteTodo,
  replaceTodos,
  toggleTodo,
} from "@/features/todos/api/commands";
import { ask, message } from "@tauri-apps/plugin-dialog";
import { exportTodos, importTodos } from "@/features/todos/api/transfer";
import { loadTodos, saveTodos } from "@/features/todos/api/storage";
import type { Todo } from "@/features/todos/types";
import styles from "./TodoList.module.scss";

export function TodoList() {
  const [todos, setTodos] = useState<Todo[]>([]);
  const [loaded, setLoaded] = useState(false);
  const [location, setLocation] = useState("");
  const [title, setTitle] = useState("");
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");

  // 起動時に保存ファイルを読み込み、Rust 側の状態にも流し込む。
  useEffect(() => {
    loadTodos()
      .then(async (saved) => {
        await replaceTodos(saved);
        setTodos(saved);
        setLoaded(true);
      })
      .catch((e) => setError(`読み込みに失敗しました: ${String(e)}`));

    appDataDir()
      .then((dir) => setLocation(`${dir}/todos.json`))
      .catch(() => setLocation(""));
  }, []);

  // 変更のたびに保存する。読み込みが終わる前に空の一覧を保存すると
  // 既存のファイルを消してしまうので、loaded になってから保存する。
  useEffect(() => {
    if (!loaded) return;
    saveTodos(todos).catch((e) => setError(`保存に失敗しました: ${String(e)}`));
  }, [todos, loaded]);

  const handleAdd = async () => {
    try {
      const todo = await addTodo(title);
      setTodos((prev) => [...prev, todo]);
      setTitle("");
      setError("");
    } catch (e) {
      // Rust 側の AppError は Serialize でそのまま文字列化されて渡ってくる。
      setError(String(e));
    }
  };

  const handleToggle = async (id: number) => {
    try {
      const updated = await toggleTodo(id);
      setTodos((prev) => prev.map((t) => (t.id === id ? updated : t)));
      setError("");
    } catch (e) {
      setError(String(e));
    }
  };

  const handleExport = async () => {
    try {
      const path = await exportTodos(todos);
      // キャンセルされた場合は null。メッセージは出さずに戻る。
      if (path === null) return;
      setNotice(`書き出しました: ${path}`);
      setError("");
    } catch (e) {
      setError(`書き出しに失敗しました: ${String(e)}`);
    }
  };

  const handleImport = async () => {
    let imported: Todo[] | null;
    try {
      imported = await importTodos();
    } catch (e) {
      // 壊れた JSON や形が違うファイルは、ダイアログで理由を知らせる。
      await message(`読み込めませんでした。\n${String(e)}`, {
        title: "読み込みエラー",
        kind: "error",
      });
      return;
    }
    // キャンセルされた場合は null。何もせずに戻る。
    if (imported === null) return;

    // 今の一覧を置き換えるので、確認してから反映する。
    const ok = await ask(
      `現在の ${todos.length} 件を、読み込んだ ${imported.length} 件で置き換えます。よろしいですか？`,
      { title: "読み込みの確認", kind: "warning" }
    );
    if (!ok) return;

    try {
      await replaceTodos(imported);
      setTodos(imported);
      setNotice(`${imported.length} 件を読み込みました`);
      setError("");
    } catch (e) {
      setError(`反映に失敗しました: ${String(e)}`);
    }
  };

  const handleDelete = async (id: number) => {
    try {
      await deleteTodo(id);
      setTodos((prev) => prev.filter((t) => t.id !== id));
      setError("");
    } catch (e) {
      setError(String(e));
    }
  };

  return (
    <section className={styles.todoList}>
      {error && <p className={styles.error}>{error}</p>}
      <div className={styles.form}>
        <input
          type="text"
          className={styles.input}
          placeholder="やることを入力"
          value={title}
          onChange={(e) => setTitle(e.target.value)}
        />
        <button type="button" className={styles.addButton} onClick={handleAdd}>
          追加
        </button>
      </div>
      <ul className={styles.items}>
        {todos.map((todo) => (
          <li key={todo.id} className={styles.item}>
            <label className={styles.itemLabel}>
              <input
                type="checkbox"
                checked={todo.done}
                onChange={() => handleToggle(todo.id)}
              />
              <span className={todo.done ? styles.itemTitleDone : styles.itemTitle}>
                {todo.title}
              </span>
            </label>
            <button
              type="button"
              className={styles.deleteButton}
              onClick={() => handleDelete(todo.id)}
            >
              削除
            </button>
          </li>
        ))}
      </ul>
      <div className={styles.transfer}>
        <button type="button" className={styles.addButton} onClick={handleExport}>
          書き出す
        </button>
        <button type="button" className={styles.addButton} onClick={handleImport}>
          読み込む
        </button>
      </div>
      {notice && <p className={styles.notice}>{notice}</p>}
      {location && <p className={styles.location}>保存先: {location}</p>}
    </section>
  );
}
