import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { AuthorList } from "./AuthorList";

vi.mock("@/features/users/api/fetchUsers", () => ({
  fetchUsers: vi.fn().mockResolvedValue([
    { id: 1, name: "Alice", username: "alice" },
    { id: 2, name: "Bob", username: "bob" },
  ]),
}));

describe("AuthorList", () => {
  it("直接 <AuthorList /> を render すると React が警告を出し、中身が描画されない（記録用）", () => {
    // AuthorList は async 関数コンポーネント（サーバーコンポーネント）なので、
    // 通常のクライアント向け render() に JSX としてそのまま渡しても例外は投げられない。
    // 代わりに React が console.error に警告を出し、コンポーネントの中身は描画されない。
    const consoleError = vi.spyOn(console, "error").mockImplementation(() => {});

    render(<AuthorList />);

    // React は console.error("%s is an async Client Component. ...", "<AuthorList>") のように
    // 書式文字列とコンポーネント名を別引数で渡すため、1 引数目だけを検証する。
    expect(consoleError).toHaveBeenCalledWith(
      expect.stringContaining(
        "%s is an async Client Component. Only Server Components can be async at the moment.",
      ),
      "<AuthorList>",
    );
    expect(screen.queryByText(/Alice/)).not.toBeInTheDocument();

    consoleError.mockRestore();
  });

  it("await AuthorList() の戻り値を render すると、fetchUsers の結果が表示される", async () => {
    render(await AuthorList());

    expect(screen.getByText("Alice（@alice）")).toBeInTheDocument();
    expect(screen.getByText("Bob（@bob）")).toBeInTheDocument();
  });
});
