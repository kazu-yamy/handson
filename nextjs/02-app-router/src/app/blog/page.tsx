import Link from "next/link";
import { getPosts } from "./_data/posts";
import styles from "./page.module.scss";

export default async function BlogPage() {
  const posts = await getPosts();

  return (
    <div>
      <h1 className={styles.title}>Blog</h1>
      <ul className={styles.list}>
        {posts.map((post) => (
          <li key={post.slug} className={styles.item}>
            <Link href={`/blog/${post.slug}`} className={styles.itemTitle}>
              {post.title}
            </Link>
            <p className={styles.itemBody}>{post.body}</p>
          </li>
        ))}
      </ul>
    </div>
  );
}
