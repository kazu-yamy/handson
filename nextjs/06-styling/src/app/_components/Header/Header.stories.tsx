import type { Meta, StoryObj } from "@storybook/nextjs-vite";
import { Header } from "./Header";

const meta = {
  title: "App/Header",
  component: Header,
  tags: ["autodocs"],
  parameters: {
    // Header は next/navigation の usePathname を使うため、
    // @storybook/nextjs-vite の Next.js 用モックで App Router の状態を再現する。
    nextjs: {
      appDirectory: true,
    },
    layout: "fullscreen",
  },
} satisfies Meta<typeof Header>;

export default meta;
type Story = StoryObj<typeof meta>;

// pathname を "/" にして、Home が現在ページとして強調されることを確認する。
export const Home: Story = {
  parameters: {
    nextjs: {
      navigation: {
        pathname: "/",
      },
    },
  },
};

// pathname を "/blog" にして、Blog が現在ページとして強調されることを確認する。
export const Blog: Story = {
  parameters: {
    nextjs: {
      navigation: {
        pathname: "/blog",
      },
    },
  },
};
