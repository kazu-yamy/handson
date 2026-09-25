import styles from "./page.module.scss";

export default function ContactPage() {
  return (
    <div className={styles.main}>
      <h1 className={styles.title}>Contact</h1>
      <p>
        このページは <code>src/app/(marketing)/contact/page.tsx</code> にありますが、
        URL は <code>/(marketing)</code> を含まない <code>/contact</code> になります。
      </p>
    </div>
  );
}
