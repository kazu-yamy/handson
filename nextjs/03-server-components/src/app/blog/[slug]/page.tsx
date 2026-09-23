import { notFound } from "next/navigation";
import { fetchPost, fetchPosts } from "@/features/posts/api/fetchPosts";
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
    </article>
  );
}
