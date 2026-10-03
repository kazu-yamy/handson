import styles from "./TitleBar.module.scss";

export function TitleBar() {
  return (
    <header className={styles.titleBar} data-tauri-drag-region>
      <span className={styles.label} data-tauri-drag-region>
        handson-tauri-03
      </span>
    </header>
  );
}
