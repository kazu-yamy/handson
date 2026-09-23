# handson

Claude Code にハンズオン教材を作らせるためのリポジトリです。教材はすべて静的 HTML（SCSS でスタイリング、デザインフレームワークは不使用）で構成します。

## 構成

教材 HTML 一式は `document/` 配下に集約されており、単独で静的サイトとして配信できます。

- `document/index.html` : 全トラックのカタログページ
- `document/template/lesson.html` : 教材 1 ページのひな型
- `document/assets/` : 共通 SCSS / CSS / JS
- `document/<track>/` : トラックごとの教材
  - `nextjs/` : Next.js（TypeScript + App Router、Storybook を含む）
  - `axum/` : Rust + Axum（Web API）
  - `tauri/` : Rust + Tauri（デスクトップアプリ）
  - `infra/` : Terraform + 各クラウド（Google Cloud / AWS / Azure / Cloudflare）
  - `android/` : Kotlin + Jetpack Compose
  - `ios/` : Swift + SwiftUI
- `work/` : 項目のブランチ上でのみ使う、学習者が実際に書くプロジェクトの置き場（main には統合しない）

## セットアップ

```
npm install
npm run build:css   # document/assets/scss/main.scss -> document/assets/css/main.css
npm run watch:css   # 変更を監視してビルド
```

## 教材の追加方法

教材作成の規約は [`CLAUDE.md`](./CLAUDE.md) を参照してください。
