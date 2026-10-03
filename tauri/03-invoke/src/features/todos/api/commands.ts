import { invoke } from "@tauri-apps/api/core";
import type { Todo } from "../types";

export async function addTodo(title: string): Promise<Todo> {
  return invoke<Todo>("add_todo", { title });
}

export async function listTodos(): Promise<Todo[]> {
  return invoke<Todo[]>("list_todos");
}

export async function toggleTodo(id: number): Promise<Todo> {
  return invoke<Todo>("toggle_todo", { id });
}

export async function deleteTodo(id: number): Promise<void> {
  return invoke<void>("delete_todo", { id });
}

export async function slowTask(): Promise<void> {
  return invoke<void>("slow_task");
}
