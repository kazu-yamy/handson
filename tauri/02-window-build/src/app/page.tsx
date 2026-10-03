"use client";

import { useEffect, useState } from "react";
import { getVersion } from "@tauri-apps/api/app";
import { WebviewWindow } from "@tauri-apps/api/webviewWindow";
import { TitleBar } from "./_components/TitleBar";
import styles from "./page.module.scss";

export default function Home() {
  const [version, setVersion] = useState("読み込み中...");

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

  return (
    <div className={styles.page}>
      <TitleBar />
      <h1 className={styles.title}>Hello, Tauri!</h1>
      <p className={styles.version}>{version}</p>
      <button type="button" className={styles.button} onClick={showAbout}>
        About を開く
      </button>
    </div>
  );
}
