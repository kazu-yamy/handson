import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it } from "vitest";
import { Collapsible } from "./Collapsible";

describe("Collapsible", () => {
  it("初期状態（defaultOpen 未指定）では children は表示されない", () => {
    render(
      <Collapsible title="解答例を見る">
        <p>ここに答えが入ります</p>
      </Collapsible>,
    );

    expect(screen.getByRole("button", { name: "解答例を見る" })).toHaveAttribute(
      "aria-expanded",
      "false",
    );
    // まだ DOM に存在しないことを確かめる（非表示ではなく render されていないことを見たいので queryByText を使う）。
    expect(screen.queryByText("ここに答えが入ります")).not.toBeInTheDocument();
  });

  it("クリックすると children が表示される", async () => {
    const user = userEvent.setup();
    render(
      <Collapsible title="解答例を見る">
        <p>ここに答えが入ります</p>
      </Collapsible>,
    );

    await user.click(screen.getByRole("button", { name: "解答例を見る" }));

    expect(screen.getByText("ここに答えが入ります")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "解答例を見る" })).toHaveAttribute(
      "aria-expanded",
      "true",
    );
  });

  it("defaultOpen=true のときは最初から children が表示される", () => {
    render(
      <Collapsible title="解答例を見る" defaultOpen>
        <p>ここに答えが入ります</p>
      </Collapsible>,
    );

    // すでに DOM にあるはずの要素なので getByText を使う（queryByText でもよいが、
    // 「あるはず」の要素には getBy* を使い、見つからなければテストを失敗させる）。
    expect(screen.getByText("ここに答えが入ります")).toBeInTheDocument();
  });
});
