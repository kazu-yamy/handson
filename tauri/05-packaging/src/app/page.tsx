"use client";

import { useEffect, useState } from "react";
import { invoke } from "@tauri-apps/api/core";
import { getVersion } from "@tauri-apps/api/app";
import { WebviewWindow } from "@tauri-apps/api/webviewWindow";
import { TitleBar } from "./_components/TitleBar";
import { TodoList } from "@/components/TodoList";
import { ProgressTask } from "@/components/ProgressTask";
import { UpdateChecker } from "@/components/UpdateChecker";
import styles from "./page.module.scss";

export default function Home() {
  const [version, setVersion] = useState("読み込み中...");
  const [name, setName] = useState("");
  const [greeting, setGreeting] = useState("");

  useEffect(() => {
    getVersion()
      .then((v) => setVersion(`Tauri アプリのバージョン: ${v}`))
      .catch(() => setVersion("ブラウザで実行中（Tauri API は利用できません）"));
  }, []);

  const showAbout = async () => {
    const about = await WebviewWindow.getByLabel("about");
    await about?.show();
    await about?.setFocus();
  };

  const greet = async () => {
    // Rust 側は fn greet(user_name: &str) なので、呼び出し側は camelCase の userName で渡す。
    const result = await invoke<string>("greet", { userName: name });
    setGreeting(result);
  };

  return (
    <div className={styles.page}>
      <TitleBar />
      <h1 className={styles.title}>Hello, Tauri!</h1>
      <p className={styles.version}>{version}</p>
      <button type="button" className={styles.button} onClick={showAbout}>
        About を開く
      </button>

      <section className={styles.greet}>
        <input
          type="text"
          className={styles.greetInput}
          placeholder="お名前"
          value={name}
          onChange={(e) => setName(e.target.value)}
        />
        <button type="button" className={styles.button} onClick={greet}>
          greet を呼び出す
        </button>
        {greeting && <p className={styles.greetResult}>{greeting}</p>}
      </section>

      <TodoList />
      <ProgressTask />
      <UpdateChecker />
    </div>
  );
}
