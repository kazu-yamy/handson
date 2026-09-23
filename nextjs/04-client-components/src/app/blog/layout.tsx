import Link from "next/link";
import { fetchPosts } from "@/features/posts/api/fetchPosts";
import styles from "./layout.module.scss";

export default async function BlogLayout({ children }: LayoutProps<"/blog">) {
  const posts = await fetchPosts();

  return (
    <div className={styles.layout}>
      <aside className={styles.sidebar}>
        <p className={styles.sidebarTitle}>記事一覧</p>
        <ul className={styles.sidebarList}>
          {posts.map((post) => (
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
