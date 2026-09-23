"use client";

import { useState } from "react";
import styles from "./LikeButton.module.scss";

type LikeButtonProps = {
  postId: number;
  initialCount: number;
};

// "use client" を付けたクライアントコンポーネント。
// サーバーコンポーネント（例: blog/[slug]/page.tsx）の JSX の中にそのまま置ける。
export function LikeButton({ postId, initialCount }: LikeButtonProps) {
  const [count, setCount] = useState(initialCount);

  return (
    <button
      type="button"
      className={styles.button}
      onClick={() => setCount((current) => current + 1)}
      aria-label={`記事 ${postId} にいいねする`}
    >
      👍 いいね {count}
    </button>
  );
}
