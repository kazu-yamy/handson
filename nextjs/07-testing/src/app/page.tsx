import Link from "next/link";
import { version } from "next/package.json";
import styles from "./page.module.scss";

export default function Home() {
  return (
    <div className={styles.main}>
      <h1 className={styles.title}>Hello, Next.js!</h1>
      <p className={styles.version}>Next.js v{version}</p>
      <Link href="/about" className={styles.link}>
        About ページへ
      </Link>
    </div>
  );
}
