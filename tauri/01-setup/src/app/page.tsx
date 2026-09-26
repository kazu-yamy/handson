"use client";

import { useEffect, useState } from "react";
import { getVersion } from "@tauri-apps/api/app";
import styles from "./page.module.scss";

export default function Home() {
  const [version, setVersion] = useState("読み込み中...");

  useEffect(() => {
    getVersion()
      .then((v) => setVersion(`Tauri アプリのバージョン: ${v}`))
      .catch(() => setVersion("ブラウザで実行中（Tauri API は利用できません）"));
  }, []);

  return (
    <div className={styles.page}>
      <h1 className={styles.title}>Hello, Tauri!</h1>
      <p className={styles.version}>{version}</p>
    </div>
  );
}
