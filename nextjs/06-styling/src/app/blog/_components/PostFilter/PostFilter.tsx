"use client";

import Link from "next/link";
import { useState } from "react";
import styles from "./PostFilter.module.scss";

type PostListItem = {
  slug: string;
  title: string;
  body: string;
  authorName: string;
};

type PostFilterProps = {
  posts: PostListItem[];
};

// データ取得（fetchPosts / fetchUsers）はサーバーである page.tsx が担当し、
// この PostFilter は受け取った posts を検索語で絞り込んで表示するだけの役割を持つ。
// 「データ取得はサーバー、操作はクライアント」という役割分担の例。
export function PostFilter({ posts }: PostFilterProps) {
  const [query, setQuery] = useState("");

  const normalizedQuery = query.trim().toLowerCase();
  const filteredPosts =
    normalizedQuery === ""
      ? posts
      : posts.filter((post) =>
          `${post.title} ${post.body}`.toLowerCase().includes(normalizedQuery),
        );

  return (
    <div>
      <input
        type="search"
        value={query}
        onChange={(event) => setQuery(event.target.value)}
        placeholder="記事を検索（タイトル・本文）"
        className={styles.input}
        aria-label="記事を検索"
      />
      <p className={styles.count}>
        {filteredPosts.length} / {posts.length} 件
      </p>

      <ul className={styles.list}>
        {filteredPosts.map((post) => (
          <li key={post.slug} className={styles.item}>
            <Link href={`/blog/${post.slug}`} className={styles.itemTitle}>
              {post.title}
            </Link>
            <p className={styles.itemAuthor}>by {post.authorName}</p>
            <p className={styles.itemBody}>{post.body}</p>
          </li>
        ))}
      </ul>

      {filteredPosts.length === 0 && (
        <p className={styles.empty}>「{query}」に一致する記事はありません。</p>
      )}
    </div>
  );
}
