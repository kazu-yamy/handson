import path from "node:path";
import { fileURLToPath } from "node:url";
import type { StorybookConfig } from '@storybook/nextjs-vite';

const dirname = path.dirname(fileURLToPath(import.meta.url));

const config: StorybookConfig = {
  "stories": [
    "../src/**/*.stories.@(ts|tsx)"
  ],
  "addons": [
    "@chromatic-com/storybook",
    "@storybook/addon-vitest",
    "@storybook/addon-a11y",
    "@storybook/addon-docs",
    "@storybook/addon-mcp"
  ],
  "framework": "@storybook/nextjs-vite",
  "staticDirs": [
    "../public"
  ],
  // next.config.ts の sassOptions（Turbopack / webpack 用）は Storybook の Vite ビルドには
  // 反映されないため、同じ内容を viteFinal でも設定する。
  // Next.js と Storybook で Sass の前処理設定を二重管理している点に注意（どちらかだけ変更すると片方だけ壊れる）。
  async viteFinal(config) {
    return {
      ...config,
      css: {
        ...config.css,
        preprocessorOptions: {
          ...config.css?.preprocessorOptions,
          scss: {
            ...config.css?.preprocessorOptions?.scss,
            loadPaths: [path.join(dirname, "../src/styles")],
            additionalData: '@use "variables" as *; @use "mixins" as *;',
          },
        },
      },
    };
  },
};
export default config;