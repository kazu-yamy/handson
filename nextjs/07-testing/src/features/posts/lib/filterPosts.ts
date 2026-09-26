export type FilterablePost = {
  slug: string;
  title: string;
  body: string;
  authorName: string;
};

/**
 * 記事一覧を検索語で絞り込む。
 * 大文字小文字を区別せず、title または body に検索語を含む記事だけを返す。
 * query が空文字（前後の空白のみを含む）のときは全件を返す。
 */
export function filterPosts(posts: FilterablePost[], query: string): FilterablePost[] {
  const normalizedQuery = query.trim().toLowerCase();

  if (normalizedQuery === "") {
    return posts;
  }

  return posts.filter((post) =>
    `${post.title} ${post.body}`.toLowerCase().includes(normalizedQuery),
  );
}
