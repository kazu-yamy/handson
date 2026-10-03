"use client";

import { useEffect, useState } from "react";
import {
  addTodo,
  deleteTodo,
  listTodos,
  toggleTodo,
} from "@/features/todos/api/commands";
import type { Todo } from "@/features/todos/types";
import styles from "./TodoList.module.scss";

export function TodoList() {
  const [todos, setTodos] = useState<Todo[]>([]);
  const [title, setTitle] = useState("");
  const [error, setError] = useState("");

  useEffect(() => {
    listTodos()
      .then(setTodos)
      .catch(() => setError("ブラウザで実行中（Tauri API は利用できません）"));
  }, []);

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
    </section>
  );
}
