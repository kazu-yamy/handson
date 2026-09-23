# handson

## リポジトリの目的

このリポジトリは、Claude Code に各種技術のハンズオン教材を作らせるためのスペースです。教材は静的 HTML（スタイルは SCSS、デザインフレームワークは使用しない）で構成します。

## ディレクトリ構成と命名規則

```
handson/
  CLAUDE.md
  README.md
  package.json          # scripts: build:css / watch:css（sass を devDependency）
  .gitignore
  document/               # 教材 HTML 一式。単独で静的サイトとして自己完結する
    index.html             # 全トラックのカタログページ
    assets/
      scss/                 # SCSS ソース
      css/main.css           # ビルド成果物（コミットする）
      js/main.js
    template/
      lesson.html            # 教材 1 ページのひな型
    nextjs/           # Next.js（TS + App Router）。Storybook もこのトラックの項目として扱う
    axum/             # Rust + Axum（Web API）
    tauri/            # Rust + Tauri（デスクトップアプリ、フロントは Next.js の SPA 出力）
    infra/
      terraform/
      google-cloud/
      aws/
      azure/
      cloudflare/
    android/          # Kotlin + Jetpack Compose
    ios/              # Swift + SwiftUI
  nextjs/<NN>-<slug>/     # 学習者が実際に書くプロジェクト（項目のブランチ上でのみ存在、main には統合しない）
  axum/<NN>-<slug>/
  tauri/<NN>-<slug>/
  android/<NN>-<slug>/
  ios/<NN>-<slug>/
  infra/<cloud>/<NN>-<slug>/
```

- 各トラック（`nextjs` / `axum` / `tauri` / `infra` / `android` / `ios`）直下に `README.md` を置き、トラックの概要とカリキュラム表（収録予定・済みの項目一覧）を書く。書式は「教材の構成単位」を参照。
- 1 項目（レッスン） = `document/<track>/<NN>-<slug>/index.html`（`infra` のみ `document/infra/<cloud>/<NN>-<slug>/index.html`）
  - 例: `document/nextjs/01-setup/index.html`、`document/nextjs/05-storybook/index.html`、`document/axum/01-hello-axum/index.html`、`document/infra/aws/01-vpc/index.html`
- 学習者が実際に手を動かして書くプロジェクトのコードは、`document/` ではなくリポジトリ直下のトラック別フォルダ `<track>/<NN>-<slug>/`（`infra` のみ `infra/<cloud>/<NN>-<slug>/`）に置く（詳細は「ブランチ運用」を参照）。教材内で完成コードを参照したい場合は、対応するブランチ名を教材ページに明記する。
- レッスンの HTML から共通 CSS/JS への参照は相対パスで書く（例: `../../assets/css/main.css`、`infra/<cloud>/` 配下は `../../../assets/css/main.css`）。

## 教材作成の手順

1. `document/template/lesson.html` をコピーして新しいレッスンのディレクトリ（`document/<track>/<NN>-<slug>/`）を作り、そこから書き始める。
2. 共通 CSS（`assets/css/main.css`）・共通 JS（`assets/js/main.js`）は相対パスで参照する。
3. 自前の `<style>` タグは原則書かない。スタイルが足りない場合は `document/assets/scss/` を拡張し、`npm run build:css` でビルドして `document/assets/css/main.css` に反映する。
4. 完成したら `document/index.html` のカタログと、該当トラックの `document/<track>/README.md` のカリキュラム表（項目の追記・状態の更新）を反映する。

## 教材の構成単位

- 教材は トラック > 項目（レッスン） > ステップ の 3 階層で構成する。1 つの HTML に複数の項目を詰め込まない。
- 項目 = 1 つの到達目標を持つ 30〜60 分の単位。`document/<track>/<NN>-<slug>/index.html` に 1 項目 1 ファイル、1 ブランチで作成する。
- トラックの `README.md` は「カリキュラム表」として、項目の一覧（番号・タイトル・到達目標 1 行・所要時間・状態: 準備中/作成済み）を Markdown テーブルで先に定義する。教材を書き始める前にこの表に項目を追加し、書き終えたら状態を更新する。
- 項目の中は `section.step` で 3〜8 個のステップに分け、各ステップは「やること → コード → 確認ポイント」の順で書く。1 ステップが長くなる場合は項目を分割する。
- 項目間の依存（前提となる項目）はページヘッダーの `.lesson-meta` に明記し、`.lesson-nav` で前後の項目へリンクする。
- 大きな題材（例: Axum で API サーバーを作る）は「セットアップ」「ルーティング」「JSON」「DB 接続」…のように項目に分解し、各項目単独でも動く完成状態（`<track>/<NN>-<slug>/`）を残す。

## 教材の書き方

- 冒頭に対象読者と前提知識を明記する。
- 1 レッスンは 30〜60 分で終わる分量にする（詳細は「教材の構成単位」を参照）。
- 各ステップは「やること → コード → 確認ポイント」の順に構成する。
- コマンドを載せる際は、実行結果も併せて載せる。
- バージョンは執筆時点の最新安定版を明記する。
- コピー & ペーストでそのまま動くことを最優先にする。

## 技術スタックの制約

- UI: デザインフレームワークは使用しない。スタイルは SCSS のみで書く。
- Next.js: TypeScript + App Router を使用する。
- Rust: Axum（Web API）/ Tauri（デスクトップアプリ、フロントエンドは Next.js の静的（SPA）出力）を使用する。
- infra: Terraform を用い、各クラウド（Google Cloud / AWS / Azure / Cloudflare）を対象にする。
- Android: Kotlin + Jetpack Compose + Navigation 3 + Room 3 + Hilt を使用する。サードパーティライブラリ（Retrofit 等）は使わず、公式パッケージのみで構成する。
- iOS: SwiftUI + `@Observable` + Swift Concurrency + SwiftData + Swift Testing を使用する。UIKit / Core Data / Combine は旧世代の技術として扱い、原則使用しない。

## ブランチ運用

- `main` には `document/` 配下の教材 HTML を全て統合する。`document/` は静的サイトとして単独で配信できる状態を常に保つ。
- 教材の項目（レッスン）は 1 項目 = 1 ブランチで作成する。ブランチ名は `<track>/<NN>-<slug>`。
  - 例: `nextjs/01-setup`、`nextjs/05-storybook`、`axum/01-hello-axum`、`infra/aws/01-vpc`
- 項目のブランチでは、ハンズオンで学習者が作るプロジェクト（コード）をリポジトリ直下のトラック別フォルダ `<track>/<NN>-<slug>/`（`infra` のみ `infra/<cloud>/<NN>-<slug>/`）に作成し、教材 HTML を `document/<track>/<NN>-<slug>/index.html` に書く。
- `main` への PR に含めるのは `document/` 配下の変更（レッスン HTML、カタログ、トラック README、必要なら `document/assets/`）のみ。トラック別フォルダ配下のコードはブランチ上に残し、main には統合しない。教材内で完成コードを参照したい場合は、ブランチ名を教材ページに明記する。
- 完成したら `main` に PR を出してマージする。コミットメッセージは日本語の 1 行要約 + 本文とする。

## コンポーネントのクラス名一覧

実装（`document/template/lesson.html` / `document/assets/scss/_layout.scss` / `document/assets/scss/_components.scss` / `document/assets/scss/_code.scss` / `document/assets/js/main.js`）が正。新しいレッスンを書く際は以下のマークアップをコピーして使う。

### サイト構造

ヘッダー: `.site-header` > `.site-header__brand`（サイトロゴ・リンク）+ `.site-header__actions`（テーマ切替・目次トグルなどのボタン群）。

```html
<header class="site-header">
  <a class="site-header__brand" href="../index.html">Handson Materials</a>
  <div class="site-header__actions">
    <button type="button" class="theme-toggle" data-theme-toggle aria-pressed="false">🌙 ダーク</button>
  </div>
</header>
```

`.theme-toggle`（`data-theme-toggle`）: ライト/ダーク切替。選択状態を `localStorage` に保存し、`html[data-theme]` 属性で反映する。マークアップは上記参照。

`.toc-toggle`（`data-toc-toggle`）: 960px 以下でサイドバー目次を開閉するボタン（レッスンページのみ）。

```html
<button type="button" class="toc-toggle" data-toc-toggle aria-expanded="false" aria-controls="lesson-toc" aria-label="目次を開閉">☰</button>
```

`.layout` > `.layout__sidebar` + `.layout__content`: 2 カラムレイアウト。960px 以下では 1 カラムになる。

```html
<div class="layout">
  <aside class="layout__sidebar" id="lesson-toc">...</aside>
  <main class="layout__content">...</main>
</div>
```

### 目次（TOC）

`<nav data-toc-list>` の中身は `main.js` が本文の h2/h3 から自動生成する（手書き不要）。生成されるのは `.toc__title` `.toc__list` `.toc__link`（現在位置は `.is-active`）。

```html
<aside class="layout__sidebar" id="lesson-toc">
  <p class="toc__title">目次</p>
  <nav aria-label="このレッスンの目次" data-toc-list></nav>
</aside>
```

### ページヘッダー

`.page-header` > `.breadcrumb`（`ol`/`li`、現在ページは `aria-current="page"`）+ `.lesson-title`（h1）+ `.lesson-meta` > `.lesson-meta__item`。

```html
<header class="page-header">
  <ol class="breadcrumb">
    <li><a href="../index.html">コース名</a></li>
    <li aria-current="page">Lesson 1</li>
  </ol>
  <h1 class="lesson-title">レッスンタイトル</h1>
  <ul class="lesson-meta">
    <li class="lesson-meta__item">⏱ 所要時間: 45分</li>
  </ul>
</header>
```

### 学習目標

`.objectives` > `.objectives__title` + `.objectives__list`。

```html
<section class="objectives">
  <p class="objectives__title">🎯 このレッスンで学ぶこと</p>
  <ul class="objectives__list"><li>...</li></ul>
</section>
```

### ステップ

`section.step` > `.step__number` + `.step__header`（`.step__title` h3）+ `.step__body`（内部に確認ポイント用の `.step__check` > `.step__check-title`）。

```html
<section class="step">
  <span class="step__number" aria-hidden="true">1</span>
  <div class="step__header"><h3 class="step__title">タイトル</h3></div>
  <div class="step__body">
    <p>...</p>
    <div class="step__check">
      <p class="step__check-title">✅ 確認ポイント</p>
      <p>...</p>
    </div>
  </div>
</section>
```

### コールアウト

`.callout.callout--note|tip|warning|danger` > `.callout__icon` + `.callout__body`（`.callout__title`）。

```html
<div class="callout callout--note">
  <span class="callout__icon" aria-hidden="true">ℹ️</span>
  <div class="callout__body">
    <p class="callout__title">Note</p>
    <p>...</p>
  </div>
</div>
```

### コードブロック

`.code-block`（diff は `.code-block--diff` を追加）> `.code-block__header`（`.code-block__lang`、`.code-block__filename`、`.code-block__spacer`、`button.code-block__copy[data-label]`）+ `pre > code.language-xxx`。diff 行は `span.diff-line.diff-line--add|remove`。highlight.js は cdnjs から本体スクリプトのみ読み込み、テーマ CSS は読み込まない（配色は `_code.scss` の `.hljs-*` で定義済み）。

```html
<div class="code-block">
  <div class="code-block__header">
    <span class="code-block__lang">TypeScript</span>
    <span class="code-block__filename">app/page.tsx</span>
    <span class="code-block__spacer"></span>
    <button type="button" class="code-block__copy" data-label="コピー">コピー</button>
  </div>
  <pre><code class="language-typescript">...</code></pre>
</div>
```

```html
<div class="code-block code-block--diff">
  <pre><span class="diff-line diff-line--remove">- old</span><span class="diff-line diff-line--add">+ new</span></pre>
</div>
```

### ターミナル

`.terminal` > `.terminal__header`（`.terminal__dot` × 3、`.terminal__title`）+ `.terminal__body`（`.terminal__line`、`.terminal__prompt`）。

```html
<div class="terminal">
  <div class="terminal__header">
    <span class="terminal__dot"></span><span class="terminal__dot"></span><span class="terminal__dot"></span>
    <span class="terminal__title">Terminal</span>
  </div>
  <pre class="terminal__body"><span class="terminal__line"><span class="terminal__prompt">$</span> npm run dev</span></pre>
</div>
```

### チェックリスト

`.checklist` > `.checklist__item`（`input.checklist__input` + `.checklist__label`）。

```html
<ul class="checklist">
  <li class="checklist__item">
    <input type="checkbox" id="check-1" class="checklist__input">
    <label for="check-1" class="checklist__label">...</label>
  </li>
</ul>
```

### 折りたたみ

`details.disclosure` > `summary.disclosure__summary` + `.disclosure__body`。

```html
<details class="disclosure">
  <summary class="disclosure__summary">解答例を見る</summary>
  <div class="disclosure__body">...</div>
</details>
```

### 前後ナビ

`.lesson-nav` > `.lesson-nav__link.lesson-nav__link--prev|next` > `.lesson-nav__direction` + `.lesson-nav__title`。

```html
<nav class="lesson-nav" aria-label="前後のレッスン">
  <a class="lesson-nav__link lesson-nav__link--prev" href="./lesson-01.html">
    <span class="lesson-nav__direction">← 前のレッスン</span>
    <span class="lesson-nav__title">Lesson 1</span>
  </a>
</nav>
```

## ビルドコマンド

```
npm install
npm run build:css   # document/assets/scss/main.scss -> document/assets/css/main.css
npm run watch:css   # 変更を監視してビルド
```
