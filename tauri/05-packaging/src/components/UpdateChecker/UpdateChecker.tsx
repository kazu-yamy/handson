"use client";

import { useState } from "react";
import { check, type Update } from "@tauri-apps/plugin-updater";
import { relaunch } from "@tauri-apps/plugin-process";
import styles from "./UpdateChecker.module.scss";

type Status = "idle" | "checking" | "available" | "upToDate" | "installing" | "error";

export function UpdateChecker() {
  const [status, setStatus] = useState<Status>("idle");
  const [update, setUpdate] = useState<Update | null>(null);
  const [message, setMessage] = useState("");

  const handleCheck = async () => {
    setStatus("checking");
    setMessage("");
    try {
      // endpoints の latest.json を取得し、現在のバージョンより新しければ Update を返す。
      const found = await check();
      if (found) {
        setUpdate(found);
        setStatus("available");
        setMessage(`新しいバージョン ${found.version} があります`);
      } else {
        setUpdate(null);
        setStatus("upToDate");
        setMessage("最新のバージョンです");
      }
    } catch (e) {
      setStatus("error");
      setMessage(String(e));
    }
  };

  const handleInstall = async () => {
    if (!update) return;
    setStatus("installing");
    setMessage("ダウンロードとインストールを実行中...");
    try {
      // 署名の検証に成功したものだけがインストールされる（pubkey と .sig の照合）。
      await update.downloadAndInstall();
      await relaunch();
    } catch (e) {
      setStatus("error");
      setMessage(String(e));
    }
  };

  const busy = status === "checking" || status === "installing";

  return (
    <section className={styles.updateChecker}>
      <button
        type="button"
        className={styles.button}
        onClick={handleCheck}
        disabled={busy}
      >
        更新を確認
      </button>

      {status === "available" && (
        <button
          type="button"
          className={styles.button}
          onClick={handleInstall}
          disabled={busy}
        >
          インストールして再起動
        </button>
      )}

      {message && (
        <p className={status === "error" ? styles.error : styles.message}>
          {message}
        </p>
      )}
    </section>
  );
}
