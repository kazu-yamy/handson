"use client";

import { useEffect, useState } from "react";
import { listen } from "@tauri-apps/api/event";
import { slowTask } from "@/features/todos/api/commands";
import styles from "./ProgressTask.module.scss";

export function ProgressTask() {
  const [progress, setProgress] = useState<number | null>(null);
  const [running, setRunning] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    const unlistenPromise = listen<number>("progress", (event) => {
      setProgress(event.payload);
    });

    // クリーンアップで unlisten しないと、再マウント時にリスナーが二重登録される。
    return () => {
      unlistenPromise.then((unlisten) => unlisten());
    };
  }, []);

  const handleRun = async () => {
    setRunning(true);
    setProgress(0);
    setError("");
    try {
      await slowTask();
    } catch (e) {
      setError(String(e));
    } finally {
      setRunning(false);
    }
  };

  return (
    <section className={styles.progressTask}>
      <button
        type="button"
        className={styles.button}
        onClick={handleRun}
        disabled={running}
      >
        重い処理を実行
      </button>
      {error && <p className={styles.error}>{error}</p>}
      {progress !== null && (
        <div className={styles.barTrack}>
          <div className={styles.barFill} style={{ width: `${progress}%` }} />
          <span className={styles.barLabel}>{progress}%</span>
        </div>
      )}
    </section>
  );
}
