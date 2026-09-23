import { notFound } from "next/navigation";
import { POSTS, getPost } from "../_data/posts";
import styles from "./page.module.scss";

export function generateStaticParams() {
  return POSTS.map((post) => ({ slug: post.slug }));
}

export default async function BlogPostPage({
  params,
}: PageProps<"/blog/[slug]">) {
  const { slug } = await params;
  const post = await getPost(slug);

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
