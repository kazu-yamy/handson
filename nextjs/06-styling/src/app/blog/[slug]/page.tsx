import { notFound } from "next/navigation";
import { fetchPost, fetchPosts } from "@/features/posts/api/fetchPosts";
import { LikeButton } from "@/components/LikeButton";
import styles from "./page.module.scss";

export async function generateStaticParams() {
  const posts = await fetchPosts();
  return posts.map((post) => ({ slug: post.slug }));
}

export default async function BlogPostPage({
  params,
}: PageProps<"/blog/[slug]">) {
  const { slug } = await params;
  const post = await fetchPost(slug);

  if (!post) {
    notFound();
  }

  return (
    <article>
      <h1 className={styles.title}>{post.title}</h1>
      <p className={styles.body}>{post.body}</p>
      {/*
        LikeButton は "use client" コンポーネント。
        サーバーコンポーネントである BlogPostPage の中に、他の要素と同じようにそのまま置ける。
        post.id を初期値の計算に使っているだけで、実際のいいね数は API から取得していない
        （このレッスンでは状態管理そのものが主題のため、ダミーの初期値を渡している）。
      */}
      <LikeButton postId={post.id} initialCount={post.id * 3} />
    </article>
  );
}
