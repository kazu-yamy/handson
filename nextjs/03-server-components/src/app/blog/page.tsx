import Link from "next/link";
import { Suspense } from "react";
import { fetchPosts } from "@/features/posts/api/fetchPosts";
import { fetchUsers } from "@/features/users/api/fetchUsers";
import { AuthorList } from "./_components/AuthorList";
import styles from "./page.module.scss";

export default async function BlogPage() {
  // 投稿と著者を直列に await すると 2 回分の待ち時間がかかる。
  // Promise.all で同時に投げて、両方の完了を 1 回の待ち時間にまとめる。
  const [posts, users] = await Promise.all([fetchPosts(), fetchUsers()]);

  const authorNameById = new Map(users.map((user) => [user.id, user.name]));

  return (
    <div>
      <h1 className={styles.title}>Blog</h1>
      <ul className={styles.list}>
        {posts.map((post) => (
          <li key={post.slug} className={styles.item}>
            <Link href={`/blog/${post.slug}`} className={styles.itemTitle}>
              {post.title}
            </Link>
            <p className={styles.itemAuthor}>
              by {authorNameById.get(post.userId) ?? "unknown"}
            </p>
            <p className={styles.itemBody}>{post.body}</p>
          </li>
        ))}
      </ul>

      {/*
        AuthorList は fetchUsers() を内部で再度呼んでいるが、
        同一レンダーパス中の同一 URL・オプションの GET fetch は Next.js が自動的にメモ化するため、
        実際に発生する外部リクエストは 1 回だけになる。
        Suspense でストリーミングされる境界であることを示すため、あえて別コンポーネントに切り出している。
      */}
      <Suspense fallback={<p className={styles.authorsLoading}>投稿者一覧を読み込み中...</p>}>
        <AuthorList />
      </Suspense>
    </div>
  );
}
