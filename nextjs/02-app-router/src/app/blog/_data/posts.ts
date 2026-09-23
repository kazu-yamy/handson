export type Post = {
  slug: string;
  title: string;
  body: string;
};

// サイドバー（layout.tsx）用の一覧はここから直接参照する（遅延なし）。
// loading.tsx の動作を見せるための遅延は getPosts / getPost にのみ入れる。
export const POSTS: Post[] = [
  {
    slug: "app-router-basics",
    title: "App Router の基礎",
    body: "src/app/ 配下のフォルダ構成がそのまま URL になります。page.tsx がそのルートの画面です。",
  },
  {
    slug: "nested-layouts",
    title: "ネストしたレイアウト",
    body: "layout.tsx は同じ階層以下の全ページに適用されます。ルートレイアウトと組み合わせて共通 UI を組み立てます。",
  },
  {
    slug: "dynamic-routes",
    title: "動的ルート",
    body: "[slug] のようにフォルダ名を角括弧で囲むと、URL の一部を受け取れる動的ルートになります。",
  },
];

function delay(ms: number) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

export async function getPosts(): Promise<Post[]> {
  await delay(1000);
  return POSTS;
}

export async function getPost(slug: string): Promise<Post | undefined> {
  await delay(1000);
  return POSTS.find((post) => post.slug === slug);
}
