import { Suspense } from "react";
import { fetchPosts } from "@/features/posts/api/fetchPosts";
import { fetchUsers } from "@/features/users/api/fetchUsers";
import { Collapsible } from "@/components/Collapsible";
import { AuthorList } from "./_components/AuthorList";
import { PostFilter } from "./_components/PostFilter";
import styles from "./page.module.scss";

export default async function BlogPage() {
  // 投稿と著者を直列に await すると 2 回分の待ち時間がかかる。
  // Promise.all で同時に投げて、両方の完了を 1 回の待ち時間にまとめる。
  const [posts, users] = await Promise.all([fetchPosts(), fetchUsers()]);

  const authorNameById = new Map(users.map((user) => [user.id, user.name]));

  // PostFilter（クライアントコンポーネント）に渡す props は、
  // 文字列・数値などシリアライズ可能な値だけにする。著者名はここで解決してから渡す。
  const postsWithAuthor = posts.map((post) => ({
    slug: post.slug,
    title: post.title,
    body: post.body,
    authorName: authorNameById.get(post.userId) ?? "unknown",
  }));

  return (
    <div>
      <h1 className={styles.title}>Blog</h1>

      {/*
        データ取得（fetchPosts / fetchUsers）はここサーバーコンポーネントで行い、
        一覧の描画と検索操作は PostFilter（クライアントコンポーネント）に委ねる。
      */}
      <PostFilter posts={postsWithAuthor} />

      {/*
        Collapsible（クライアントコンポーネント）の children に、
        async なサーバーコンポーネント（AuthorList）を渡している。
        children の中身を組み立てるのは呼び出し側の page.tsx（サーバー）であり、
        Collapsible 自身は中身がサーバー製かクライアント製かを意識しない。
        AuthorList は fetchUsers() を内部で再度呼んでいるが、
        同一レンダーパス中の同一 URL・オプションの GET fetch は Next.js が自動的にメモ化するため、
        実際に発生する外部リクエストは 1 回だけになる。
        Suspense でストリーミングされる境界であることを示すため、あえて別コンポーネントに切り出している。
      */}
      <Collapsible title="著者一覧" defaultOpen>
        <Suspense fallback={<p className={styles.authorsLoading}>投稿者一覧を読み込み中...</p>}>
          <AuthorList />
        </Suspense>
      </Collapsible>
    </div>
  );
}
