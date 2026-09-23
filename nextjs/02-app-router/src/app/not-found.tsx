import Link from "next/link";
import styles from "./not-found.module.scss";

export default function NotFound() {
  return (
    <div className={styles.main}>
      <h1 className={styles.title}>404 - Not Found</h1>
      <p>お探しのページは見つかりませんでした。</p>
      <Link href="/" className={styles.link}>
        トップページへ戻る
      </Link>
    </div>
  );
}
