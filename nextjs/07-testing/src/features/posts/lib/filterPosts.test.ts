import { describe, expect, it } from "vitest";
import { filterPosts, type FilterablePost } from "./filterPosts";

const posts: FilterablePost[] = [
  { slug: "1", title: "Next.js 入門", body: "App Router を使う", authorName: "Alice" },
  { slug: "2", title: "Rust ではじめる Web API", body: "Axum を使う", authorName: "Bob" },
  { slug: "3", title: "TypeScript の型システム", body: "型推論について", authorName: "Carol" },
];

describe("filterPosts", () => {
  it("query が空文字のときは全件を返す", () => {
    expect(filterPosts(posts, "")).toEqual(posts);
  });

  it("query が空白のみのときも全件を返す", () => {
    expect(filterPosts(posts, "   ")).toEqual(posts);
  });

  it("title に部分一致する記事だけを返す", () => {
    const result = filterPosts(posts, "Next");

    expect(result).toEqual([posts[0]]);
  });

  it("大文字小文字を区別しない", () => {
    const result = filterPosts(posts, "typescript");

    expect(result).toEqual([posts[2]]);
  });

  it("body に部分一致する記事だけを返す", () => {
    const result = filterPosts(posts, "Axum");

    expect(result).toEqual([posts[1]]);
  });

  it("該当する記事がないときは空配列を返す", () => {
    expect(filterPosts(posts, "存在しない記事")).toEqual([]);
  });
});
