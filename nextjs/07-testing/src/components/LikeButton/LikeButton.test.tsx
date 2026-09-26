import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { LikeButton } from "./LikeButton";

describe("LikeButton", () => {
  it("初期表示で initialCount が表示される", () => {
    render(<LikeButton postId={1} initialCount={3} />);

    expect(screen.getByRole("button", { name: "記事 1 にいいねする" })).toHaveTextContent(
      "👍 いいね 3",
    );
  });

  it("クリックするたびにカウントが 1 ずつ増える", async () => {
    const user = userEvent.setup();
    render(<LikeButton postId={1} initialCount={0} />);

    const button = screen.getByRole("button", { name: "記事 1 にいいねする" });
    await user.click(button);
    await user.click(button);

    expect(button).toHaveTextContent("👍 いいね 2");
  });

  it("クリックすると onLiked が更新後のカウントで 1 回だけ呼ばれる", async () => {
    const user = userEvent.setup();
    const onLiked = vi.fn();
    render(<LikeButton postId={1} initialCount={0} onLiked={onLiked} />);

    await user.click(screen.getByRole("button", { name: "記事 1 にいいねする" }));

    expect(onLiked).toHaveBeenCalledTimes(1);
    expect(onLiked).toHaveBeenCalledWith(1);
  });
});
