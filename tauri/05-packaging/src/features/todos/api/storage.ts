import {
  BaseDirectory,
  exists,
  mkdir,
  readTextFile,
  writeTextFile,
} from "@tauri-apps/plugin-fs";
import type { Todo } from "../types";
import { parseTodos } from "./validate";

// AppData（macOS では ~/Library/Application Support/<identifier>）の直下に保存する。
const FILE_NAME = "todos.json";
const OPTIONS = { baseDir: BaseDirectory.AppData } as const;

export async function saveTodos(todos: Todo[]): Promise<void> {
  // AppData のフォルダは初回起動時には存在しないので、先に作っておく。
  // 既にあっても recursive: true なら失敗しない。
  await mkdir("", { ...OPTIONS, recursive: true });
  await writeTextFile(FILE_NAME, JSON.stringify(todos, null, 2), OPTIONS);
}

export async function loadTodos(): Promise<Todo[]> {
  if (!(await exists(FILE_NAME, OPTIONS))) {
    return [];
  }
  const text = await readTextFile(FILE_NAME, OPTIONS);
  return parseTodos(text);
}
