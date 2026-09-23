import { NextResponse } from "next/server";

// Route Handler。ブラウザや curl など、サーバーコンポーネントの外側から呼ばれる
// 独立した Web API エンドポイントとして動く。サーバーコンポーネントから直接この URL を
// fetch する必要はない（fetchPosts() は JSONPlaceholder を直接叩いている）。
export async function GET() {
  const res = await fetch("https://jsonplaceholder.typicode.com/posts?_limit=5");

  if (!res.ok) {
    return NextResponse.json(
      { error: `記事一覧の取得に失敗しました (status: ${res.status})` },
      { status: res.status },
    );
  }

  const posts = await res.json();

  return NextResponse.json({ posts });
}
