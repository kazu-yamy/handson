import Link from "next/link";
import { POSTS } from "./_data/posts";
import styles from "./layout.module.scss";

export default function BlogLayout({ children }: LayoutProps<"/blog">) {
  return (
    <div className={styles.layout}>
      <aside className={styles.sidebar}>
        <p className={styles.sidebarTitle}>記事一覧</p>
        <ul className={styles.sidebarList}>
          {POSTS.map((post) => (
            <li key={post.slug}>
              <Link href={`/blog/${post.slug}`}>{post.title}</Link>
            </li>
          ))}
        </ul>
      </aside>
      <div className={styles.content}>{children}</div>
    </div>
  );
}
