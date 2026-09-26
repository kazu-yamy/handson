import type { Meta, StoryObj } from "@storybook/nextjs-vite";
import { Button } from "./Button";

const meta = {
  title: "Components/Button",
  component: Button,
  tags: ["autodocs"],
  argTypes: {
    variant: {
      control: "radio",
      options: ["primary", "secondary", "ghost"],
    },
    size: {
      control: "radio",
      options: ["sm", "md"],
    },
  },
  args: {
    children: "ボタン",
  },
} satisfies Meta<typeof Button>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Primary: Story = {
  args: {
    variant: "primary",
  },
};

export const Secondary: Story = {
  args: {
    variant: "secondary",
  },
};

export const Ghost: Story = {
  args: {
    variant: "ghost",
  },
};

// variant（3 種類）× size（2 種類）の全組み合わせを 1 つのストーリーに並べて確認する。
export const AllVariants: Story = {
  render: (args) => (
    <div style={{ display: "flex", flexWrap: "wrap", gap: "0.75rem" }}>
      {(["primary", "secondary", "ghost"] as const).map((variant) =>
        (["sm", "md"] as const).map((size) => (
          <Button key={`${variant}-${size}`} {...args} variant={variant} size={size}>
            {variant} / {size}
          </Button>
        )),
      )}
    </div>
  ),
};
