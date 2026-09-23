import type { Post } from "../types";

const API_BASE = "https://jsonplaceholder.typicode.com";

type JsonPlaceholderPost = {
  id: number;
  userId: number;
  title: string;
  body: string;
};

function toPost(raw: JsonPlaceholderPost): Post {
  return {
    id: raw.id,
    userId: raw.userId,
    // JSONPlaceholder の id をそのまま動的ルート [slug] の値として使う。
    slug: String(raw.id),
    title: raw.title,
    body: raw.body,
  };
}

/**
 * 記事一覧を取得する。
 * `next.revalidate: 60` により ISR（Incremental Static Regeneration）になる:
 * ビルド時に一度取得した結果を最大 60 秒間キャッシュして返し、
 * 60 秒経過後の最初のアクセスをきっかけにバックグラウンドで再取得・再生成する。
 */
export async function fetchPosts(): Promise<Post[]> {
  const res = await fetch(`${API_BASE}/posts?_limit=5`, {
    next: { revalidate: 60 },
  });

  if (!res.ok) {
    throw new Error(`記事一覧の取得に失敗しました (status: ${res.status})`);
  }

  const posts: JsonPlaceholderPost[] = await res.json();

  return posts.map(toPost);
}

/**
 * 記事を 1 件取得する。存在しない id の場合は null を返す（呼び出し側で notFound() する）。
 */
export async function fetchPost(id: string): Promise<Post | null> {
  const res = await fetch(`${API_BASE}/posts/${id}`, {
    next: { revalidate: 60 },
  });

  if (res.status === 404) {
    return null;
  }

  if (!res.ok) {
    throw new Error(`記事の取得に失敗しました (status: ${res.status})`);
  }

  const post: JsonPlaceholderPost = await res.json();

  return toPost(post);
}
