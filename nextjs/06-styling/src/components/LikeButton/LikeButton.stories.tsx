import type { Meta, StoryObj } from "@storybook/nextjs-vite";
import { expect, fn, userEvent, within } from "storybook/test";
import { LikeButton } from "./LikeButton";

const meta = {
  title: "Components/LikeButton",
  component: LikeButton,
  tags: ["autodocs"],
  argTypes: {
    initialCount: {
      control: { type: "number", min: 0 },
    },
  },
  args: {
    // fn() で渡すと、クリックのたびに呼び出しが Storybook の Actions パネルに記録される。
    onLiked: fn(),
  },
} satisfies Meta<typeof LikeButton>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Default: Story = {
  args: {
    postId: 1,
    initialCount: 0,
  },
};

export const WithInitialCount: Story = {
  args: {
    postId: 1,
    initialCount: 10,
  },
};

// play 関数でボタンをクリックし、カウントが増えることと onLiked が呼ばれることを検証する。
export const ClickIncrementsCount: Story = {
  args: {
    postId: 1,
    initialCount: 0,
  },
  play: async ({ canvasElement, args }) => {
    const canvas = within(canvasElement);
    const button = canvas.getByRole("button");

    await expect(button).toHaveTextContent("👍 いいね 0");

    await userEvent.click(button);

    await expect(button).toHaveTextContent("👍 いいね 1");
    await expect(args.onLiked).toHaveBeenCalledWith(1);
    await expect(args.onLiked).toHaveBeenCalledTimes(1);
  },
};
