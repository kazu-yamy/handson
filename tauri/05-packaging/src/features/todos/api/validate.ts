import type { Todo } from "../types";

/// JSON 文字列を Todo の配列として検証する。
/// 壊れた JSON は JSON.parse が SyntaxError を投げ、形が違うものは Error を投げる。
export function parseTodos(text: string): Todo[] {
  const value: unknown = JSON.parse(text);
  if (!Array.isArray(value)) {
    throw new Error("Todo の配列（[ ... ]）ではありません");
  }

  value.forEach((item: unknown, index: number) => {
    const at = `${index + 1} 件目`;
    if (typeof item !== "object" || item === null) {
      throw new Error(`${at}: オブジェクトではありません`);
    }
    const todo = item as Record<string, unknown>;
    if (typeof todo.id !== "number" || !Number.isInteger(todo.id)) {
      throw new Error(`${at}: id が整数ではありません`);
    }
    if (typeof todo.title !== "string") {
      throw new Error(`${at}: title が文字列ではありません`);
    }
    if (typeof todo.done !== "boolean") {
      throw new Error(`${at}: done が真偽値ではありません`);
    }
    // Rust 側の Todo は createdAt も必須なので、ここで確かめておく。
    if (typeof todo.createdAt !== "number") {
      throw new Error(`${at}: createdAt が数値ではありません`);
    }
  });

  return value as Todo[];
}
