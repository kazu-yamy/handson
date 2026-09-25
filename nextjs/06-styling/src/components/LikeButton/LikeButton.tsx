"use client";

import { useState } from "react";
import { Button } from "@/components/Button";
import styles from "./LikeButton.module.scss";

type LikeButtonProps = {
  postId: number;
  initialCount: number;
  onLiked?: (count: number) => void;
};

// "use client" を付けたクライアントコンポーネント。
// サーバーコンポーネント（例: blog/[slug]/page.tsx）の JSX の中にそのまま置ける。
//
// onLiked は Storybook の Actions パネルで動作を可視化するために用意したコールバックで、
// 04 の教材で確認したとおり、サーバーコンポーネントからこの props に関数を渡すことはできない
// （渡すと "Event handlers cannot be passed to Client Component props." になる）。
// そのためアプリ本体（blog/[slug]/page.tsx）では onLiked を渡していない。
export function LikeButton({ postId, initialCount, onLiked }: LikeButtonProps) {
  const [count, setCount] = useState(initialCount);

  const handleClick = () => {
    const next = count + 1;
    setCount(next);
    onLiked?.(next);
  };

  return (
    <Button
      variant="secondary"
      className={styles.likeButton}
      onClick={handleClick}
      aria-label={`記事 ${postId} にいいねする`}
    >
      👍 いいね {count}
    </Button>
  );
}
