import styles from "./page.module.scss";

export default function About() {
  return (
    <div className={styles.page}>
      <h1 className={styles.title}>About</h1>
      <p className={styles.text}>handson-tauri-02 の 2 つ目のウィンドウです。</p>
    </div>
  );
}
