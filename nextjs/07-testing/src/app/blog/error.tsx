"use client"; // error.tsx はエラーバウンダリなので必ず Client Component にする

import { useEffect } from "react";
import styles from "./error.module.scss";

export default function BlogError({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  useEffect(() => {
    // エラーレポーティングサービスに送る想定。ここでは開発用にログのみ。
    console.error(error);
  }, [error]);

  return (
    <div className={styles.error}>
      <p className={styles.errorTitle}>記事の読み込みに失敗しました</p>
      <p className={styles.errorMessage}>{error.message}</p>
      <button type="button" className={styles.errorButton} onClick={() => reset()}>
        もう一度読み込む
      </button>
    </div>
  );
}
