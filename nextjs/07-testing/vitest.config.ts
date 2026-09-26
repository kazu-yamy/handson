import path from 'node:path';
import { fileURLToPath } from 'node:url';

import { defineConfig } from 'vitest/config';

import { storybookTest } from '@storybook/addon-vitest/vitest-plugin';

import { playwright } from '@vitest/browser-playwright';

const dirname =
  typeof __dirname !== 'undefined' ? __dirname : path.dirname(fileURLToPath(import.meta.url));

// More info at: https://storybook.js.org/docs/next/writing-tests/integrations/vitest-addon
export default defineConfig({
  test: {
    // カバレッジはプロジェクト共通の設定として一箇所にまとめる（projects 個別には書かない）。
    // src/**/*.stories.tsx（Storybook 用の見本コード）と src/app/**（ルーティング専用。
    // ページ本体のロジックは features/components 側でテストする）は対象外にする。
    coverage: {
      provider: 'v8',
      include: ['src/**'],
      exclude: ['src/**/*.stories.tsx', 'src/app/**'],
    },
    projects: [
      {
        // ブラウザに依存しない、通常のユニット / コンポーネントテスト（jsdom 環境）。
        resolve: {
          alias: {
            '@': path.join(dirname, 'src'),
          },
        },
        test: {
          name: 'unit',
          environment: 'jsdom',
          include: ['src/**/*.test.{ts,tsx}'],
          setupFiles: [path.join(dirname, 'vitest.setup.ts')],
          css: {
            modules: {
              // CSS Modules のクラス名をハッシュ化せずそのまま返す。
              // テストでは実際のハッシュ値ではなく、元のクラス名（styles.button など）で
              // アサーションしたいことが多いため。
              classNameStrategy: 'non-scoped',
            },
          },
        },
      },
      {
        extends: true,
        plugins: [
          // The plugin will run tests for the stories defined in your Storybook config
          // See options at: https://storybook.js.org/docs/next/writing-tests/integrations/vitest-addon#storybooktest
          storybookTest({ configDir: path.join(dirname, '.storybook') }),
        ],
        test: {
          name: 'storybook',
          browser: {
            enabled: true,
            headless: true,
            provider: playwright({}),
            instances: [{ browser: 'chromium' }],
          },
        },
      },
    ],
  },
});
