import { afterEach, describe, expect, it, vi } from "vitest";
import { fetchPosts } from "./fetchPosts";

// レスポンスの一部（.ok と .json() だけ）を満たすダミー。fetch の戻り値全体（Response 型）を
// 作り込む必要はなく、fetchPosts が実際に使っているプロパティだけあれば十分。
function createResponse(overrides: { ok: boolean; status?: number; body?: unknown }) {
  return {
    ok: overrides.ok,
    status: overrides.status ?? (overrides.ok ? 200 : 500),
    json: async () => overrides.body,
  } as Response;
}

describe("fetchPosts", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("正常系: JSONPlaceholder のレスポンスを Post[] に変換して返す", async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      createResponse({
        ok: true,
        body: [
          { id: 1, userId: 10, title: "タイトル1", body: "本文1" },
          { id: 2, userId: 20, title: "タイトル2", body: "本文2" },
        ],
      }),
    );
    vi.stubGlobal("fetch", fetchMock);

    const posts = await fetchPosts();

    expect(posts).toEqual([
      { id: 1, userId: 10, slug: "1", title: "タイトル1", body: "本文1" },
      { id: 2, userId: 20, slug: "2", title: "タイトル2", body: "本文2" },
    ]);
    expect(fetchMock).toHaveBeenCalledWith(
      "https://jsonplaceholder.typicode.com/posts?_limit=5",
      { next: { revalidate: 60 } },
    );
  });

  it("異常系: res.ok が false のときは status を含むエラーを投げる", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(createResponse({ ok: false, status: 503 })),
    );

    await expect(fetchPosts()).rejects.toThrow("記事一覧の取得に失敗しました (status: 503)");
  });
});
