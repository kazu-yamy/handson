"use client";

import { useReducer } from "react";
import styles from "./LikeButton.module.scss";

type LikeButtonProps = {
  postId: number;
  initialCount: number;
};

type State = { count: number };
type Action = { type: "increment" };

function reducer(state: State, action: Action): State {
  switch (action.type) {
    case "increment":
      return { count: state.count + 1 };
    default:
      return state;
  }
}

// LikeButton（useState 版）と同じ見た目・同じ振る舞いを useReducer で書き直したもの。
// このレッスンでは状態が「カウンター 1 個」しかないため useState で十分だが、
// 状態の更新パターンが増えたときに useReducer へ移行する感覚を掴むための比較用コンポーネント。
// 詳細ページ（[slug]/page.tsx）では引き続き LikeButton（useState 版）を使う。
export function LikeButtonReducer({ postId, initialCount }: LikeButtonProps) {
  const [state, dispatch] = useReducer(reducer, { count: initialCount });

  return (
    <button
      type="button"
      className={styles.button}
      onClick={() => dispatch({ type: "increment" })}
      aria-label={`記事 ${postId} にいいねする`}
    >
      👍 いいね {state.count}
    </button>
  );
}
