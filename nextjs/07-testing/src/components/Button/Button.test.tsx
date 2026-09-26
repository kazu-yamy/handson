import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { Button } from "./Button";

describe("Button", () => {
  it("デフォルトでは variant=primary / size=md が data-* 属性に反映される", () => {
    render(<Button>送信</Button>);

    const button = screen.getByRole("button", { name: "送信" });

    expect(button).toHaveAttribute("data-variant", "primary");
    expect(button).toHaveAttribute("data-size", "md");
  });

  it("variant / size を指定すると data-* 属性が反映される", () => {
    render(
      <Button variant="secondary" size="sm">
        いいね
      </Button>,
    );

    const button = screen.getByRole("button", { name: "いいね" });

    expect(button).toHaveAttribute("data-variant", "secondary");
    expect(button).toHaveAttribute("data-size", "sm");
  });
});
