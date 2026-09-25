# Next.js トラック

Next.js（TypeScript + App Router）を使ったフロントエンド開発を学ぶトラックです。Storybook を使ったコンポーネントカタログの構築も、このトラックの項目として扱います。

## プロジェクト構成

- `src/app/` はルーティング専用（`page.tsx` / `layout.tsx` / `loading.tsx` など）。ルートグループ `(group)/` とルート専用部品の `_components/` を使う。
- 再利用する部品は `src/components/<Name>/` に 1 コンポーネント = 1 フォルダ（`<Name>.tsx` / `.module.scss` / `.stories.tsx` / `.test.tsx` / `index.ts`）でまとめる。
- 機能単位のまとまりは `src/features/<feature>/` に `components` / `hooks` / `api` を同居させる。
- 共通 SCSS は `src/styles/`（`globals.scss` / `_variables.scss` / `_mixins.scss`）に集約する。

```
src/
  app/                    # ルーティング専用
    (marketing)/          # ルートグループ
    _components/          # ルート専用の部品
    layout.tsx
    page.tsx
    page.module.scss
  components/
    Button/
      Button.tsx
      Button.module.scss
      Button.stories.tsx
      Button.test.tsx
      index.ts
  features/
    todo/
      components/ hooks/ api/
  styles/
    globals.scss
    _variables.scss
    _mixins.scss
```

詳細は CLAUDE.md の「Next.js プロジェクトの構成規約」を参照してください。

## カリキュラム表

進捗は Obsidian の Projects/handson/progress.md で管理する。

| 番号 | タイトル | 到達目標 | 所要時間 |
| --- | --- | --- | --- |
| 01 | [環境構築とプロジェクト作成](./01-setup/index.html) | Node.js と Next.js の環境を整え、`create-next-app` でプロジェクトを作成できる | 30分 |
| 02 | [App Router の基礎とファイルベースルーティング](./02-app-router/index.html) | ページ・レイアウト・ネストルーティングを実装できる | 45分 |
| 03 | [サーバーコンポーネントとデータ取得](./03-server-components/index.html) | サーバーコンポーネントで `fetch` を使ったデータ取得ができる | 45分 |
| 04 | [クライアントコンポーネントと状態管理](./04-client-components/index.html) | `"use client"` の使い分けと `useState` などのフック活用ができる | 45分 |
| 05 | [Storybook でのコンポーネントカタログ構築](./05-storybook/index.html) | Storybook を導入し、コンポーネント単位で開発・確認できる | 45分 |
| 06 | [スタイリングとレイアウト実装](./06-styling/index.html) | CSS Modules / SCSS を使ったスタイリングができる | 30分 |
| 07 | テストの導入 | Vitest / Testing Library で基本的なテストを書ける | 45分 |
