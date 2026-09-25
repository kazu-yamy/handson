import styles from "./page.module.scss";

export default function AboutPage() {
  return (
    <div className={styles.main}>
      <h1 className={styles.title}>About</h1>
      <p>このページは about/page.tsx が /about に対応することを確かめるためのページです。</p>
    </div>
  );
}
