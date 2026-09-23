"use client";

import { useEffect, useState } from "react";
import styles from "./ThemeToggle.module.scss";

type Theme = "light" | "dark";

const STORAGE_KEY = "theme";

// サーバーで生成する HTML には localStorage の値を反映できないため、
// 初期値は必ず light 側の見た目に合わせておき、実際の値は useEffect の中で読む。
// useState(() => localStorage.getItem(...)) のように初期化子で読むと、
// サーバーが返した HTML（light 前提）とクライアントの初回レンダー結果（保存済みの dark）が
// 食い違い、ハイドレーションエラーになる。
export function ThemeToggle() {
  // サーバーで生成する HTML は必ず "light" として描画する。
  const [theme, setTheme] = useState<Theme>("light");

  // マウント後（＝ブラウザで実行されるとき）に一度だけ localStorage を読み、
  // 保存されていた値を document とステートに反映する。
  // 保存値が無い場合は、OS の配色設定（prefers-color-scheme）に従って初期テーマを決める。
  useEffect(() => {
    const stored = window.localStorage.getItem(STORAGE_KEY);
    const initial: Theme =
      stored === "light" || stored === "dark"
        ? stored
        : window.matchMedia("(prefers-color-scheme: dark)").matches
          ? "dark"
          : "light";
    // localStorage・matchMedia は React の外側にある状態なので、effect の中で読んで同期する必要がある。
    // eslint-disable-next-line react-hooks/set-state-in-effect -- 外部ストレージとの同期のため意図的
    setTheme(initial);
    document.documentElement.dataset.theme = initial;
  }, []);

  const toggleTheme = () => {
    const next: Theme = theme === "light" ? "dark" : "light";
    setTheme(next);
    document.documentElement.dataset.theme = next;
    window.localStorage.setItem(STORAGE_KEY, next);
  };

  return (
    <button
      type="button"
      className={styles.toggle}
      onClick={toggleTheme}
      aria-pressed={theme === "dark"}
    >
      {theme === "dark" ? "☀️ ライト" : "🌙 ダーク"}
    </button>
  );
}
