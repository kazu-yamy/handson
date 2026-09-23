"use client";

import { useState, type ReactNode } from "react";
import styles from "./Collapsible.module.scss";

type CollapsibleProps = {
  title: string;
  children: ReactNode;
  defaultOpen?: boolean;
};

// "use client" コンポーネントでも children は受け取れる。
// children として渡された JSX がサーバーコンポーネントであっても、
// それを組み立てるのは親（page.tsx）の役目なので、この Collapsible 自体は
// 中身がサーバーコンポーネントかクライアントコンポーネントかを知らなくてよい。
export function Collapsible({ title, children, defaultOpen = false }: CollapsibleProps) {
  const [isOpen, setIsOpen] = useState(defaultOpen);

  return (
    <div className={styles.collapsible}>
      <button
        type="button"
        className={styles.summary}
        onClick={() => setIsOpen((current) => !current)}
        aria-expanded={isOpen}
      >
        <span className={styles.icon} aria-hidden="true">
          {isOpen ? "▾" : "▸"}
        </span>
        {title}
      </button>
      {isOpen && <div className={styles.body}>{children}</div>}
    </div>
  );
}
