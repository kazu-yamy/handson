import type { Meta, StoryObj } from "@storybook/nextjs-vite";
import { Collapsible } from "./Collapsible";

// children に JSX を渡す例で共通して使うダミーの中身。
// Collapsible は Next.js のアプリ内では children としてサーバーコンポーネント（AuthorList）を
// 受け取るが、Storybook 上では Collapsible 自身の開閉の振る舞いを確認したいだけなので、
// 代わりに簡単な静的 JSX を渡している。
const sampleChildren = (
  <ul>
    <li>Leanne Graham</li>
    <li>Ervin Howell</li>
    <li>Clementine Bauch</li>
  </ul>
);

const meta = {
  title: "Components/Collapsible",
  component: Collapsible,
  tags: ["autodocs"],
  args: {
    children: sampleChildren,
  },
} satisfies Meta<typeof Collapsible>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Default: Story = {
  args: {
    title: "著者一覧",
  },
  // render 関数の引数 args から children を受け取り、そのまま Collapsible に渡す。
  render: (args) => <Collapsible {...args} />,
};

// defaultOpen で最初から開いた状態を確認する。
export const Open: Story = {
  args: {
    title: "著者一覧",
    defaultOpen: true,
  },
  render: (args) => <Collapsible {...args} />,
};
