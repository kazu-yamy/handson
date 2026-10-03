import { open, save } from "@tauri-apps/plugin-dialog";
import { readTextFile, writeTextFile } from "@tauri-apps/plugin-fs";
import type { Todo } from "../types";
import { parseTodos } from "./validate";

const JSON_FILTER = { name: "JSON", extensions: ["json"] };

/// 保存先をダイアログで選んで書き出す。キャンセルされたら null を返す。
export async function exportTodos(todos: Todo[]): Promise<string | null> {
  const path = await save({
    defaultPath: "todos.json",
    filters: [JSON_FILTER],
  });
  // キャンセル時は null が返る。何もせずに終わる。
  if (path === null) return null;

  // ダイアログで選んだパスは、fs の scope（許可パス）の外でも書き込める。
  await writeTextFile(path, JSON.stringify(todos, null, 2));
  return path;
}

/// ダイアログで JSON を選び、検証してから Todo の配列を返す。
/// キャンセルされたら null。壊れた JSON や形が違う場合は例外を投げる。
export async function importTodos(): Promise<Todo[] | null> {
  const path = await open({
    multiple: false,
    directory: false,
    filters: [JSON_FILTER],
  });
  if (path === null) return null;

  const text = await readTextFile(path);
  return parseTodos(text);
}
