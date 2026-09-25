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
4. 完成したら `document/index.html` のカタログと、該当トラックの `document/<track>/README.md` のカリキュラム表に項目を追記する。

## 教材の構成単位

- 教材は トラック > 項目（レッスン） > ステップ の 3 階層で構成する。1 つの HTML に複数の項目を詰め込まない。
- 項目 = 1 つの到達目標を持つ 30〜60 分の単位。`document/<track>/<NN>-<slug>/index.html` に 1 項目 1 ファイル、1 ブランチで作成する。
- トラックの `README.md` は「カリキュラム表」として、項目の一覧（番号・タイトル・到達目標 1 行・所要時間）を Markdown テーブルで先に定義する。教材を書き始める前にこの表に項目を追加する。進捗は Obsidian vault の `Projects/handson/progress.md` で管理する。教材を main にマージしたら、そのノートの該当項目を更新する。README のカリキュラム表とカタログには状態を書かない。カタログには作成済みの項目だけを載せる。
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

## Next.js プロジェクトの構成規約

- `src/app/` はルーティング専用（`page.tsx` / `layout.tsx` / `loading.tsx` など）とする。ルートグループ `(group)/` と、そのルート専用の部品を置く `_components/`（先頭 `_` でルート化を防ぐ）を使う。
- 再利用する部品は `src/components/<Name>/` に 1 コンポーネント = 1 フォルダで置く。`<Name>.tsx`、`<Name>.module.scss`、`<Name>.stories.tsx`（Storybook 導入後）、`<Name>.test.tsx`（テスト導入後）、`index.ts`（re-export）を同居させる。
- 機能単位のまとまりは `src/features/<feature>/` に `components` / `hooks` / `api` を同居させる。
- 共通 SCSS は `src/styles/` に集約する: `globals.scss`、`_variables.scss`、`_mixins.scss`。`next.config.ts` の `sassOptions.loadPaths: ['./src/styles']` と `additionalData` で `@use "variables" as *;` を自動プレリュードにし、各 `module.scss` から相対パスを書かないようにする（`includePaths` は Next.js 16 の sass-loader では無視されるので使わない。Storybook には継承されないため `.storybook/main.ts` の `viteFinal` にも同じ設定を書く。この設定は 06 スタイリングの項目で導入する。01〜05 は `globals.scss` のみを使う）。
- `_variables.scss` / `_mixins.scss` には CSS を出力する記述（`:root` など）を書かない。`additionalData` で全ての `module.scss` に `@use` されるため、CSS を出力するとファイルの数だけ複製される。CSS カスタムプロパティの定義は `globals.scss` に置く。
- ページ専用のスタイルは `page.module.scss` を `page.tsx` と同じ場所に置いてよい。
- エディタ表示はリポジトリ直下の `.vscode/settings.json` の file nesting で `.module.scss` / `.stories.tsx` / `.test.tsx` を `.tsx` の下にネストする。

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

## ブランチ運用

- `main` には `document/` 配下の教材 HTML を全て統合する。`document/` は静的サイトとして単独で配信できる状態を常に保つ。
- 教材の項目（レッスン）は 1 項目 = 1 ブランチで作成する。ブランチ名は `<track>/<NN>-<slug>`。
  - 例: `nextjs/01-setup`、`nextjs/05-storybook`、`axum/01-hello-axum`、`infra/aws/01-vpc`
- 項目のブランチでは、ハンズオンで学習者が作るプロジェクト（コード）をリポジトリ直下のトラック別フォルダ `<track>/<NN>-<slug>/`（`infra` のみ `infra/<cloud>/<NN>-<slug>/`）に作成し、教材 HTML を `document/<track>/<NN>-<slug>/index.html` に書く。
- `main` への PR に含めるのは `document/` 配下の変更（レッスン HTML、カタログ、トラック README、必要なら `document/assets/`）のみ。トラック別フォルダ配下のコードはブランチ上に残し、main には統合しない。教材内で完成コードを参照したい場合は、ブランチ名を教材ページに明記する。
- 完成したら PR 用ブランチ `docs/<track>-<NN>-<slug>` を `main` から切り、`git checkout <track>/<NN>-<slug> -- document/` で `document/` の変更だけを取り込んで 1 コミットにし、`main` に PR を出す。コミットメッセージは日本語の 1 行要約 + 本文とする。
- PR は **squash merge** でマージし、マージ後に `docs/` ブランチは削除する。コードを含む `<track>/<NN>-<slug>` ブランチはマージせず残す。
- 複数のセッションで並行して別トラックを作る場合は、トラックごとに `git worktree` を分ける。同じ作業ツリーでブランチを切り替え合わない。
  - 例: `git worktree add ../handson-axum -b axum/01-hello-axum main` して `../handson-axum` で Claude Code を起動する。
  - 共有ファイルは `document/index.html`（カタログ）と Obsidian の `progress.md` だけなので、PR 用ブランチは必ず最新の `main` から切る。
  - 開発サーバーのポート（Next.js 3000、Storybook 6006 など）が他セッションと重ならないよう、起動前に空きを確認する。

## 教材作成のワークフロー

1 項目は次の 4 段階で作る。各段階は別のエージェント（またはセッション）が担当し、前段の成果物だけを入力にする。

1. **実装（implementer）**: 前の項目の完成コードを出発点に、`<track>/<NN>-<slug>/` に実プロジェクトを作る。ステップごとに「作成・変更したファイルの全文（変更は前後）」「実行したコマンドと実際の出力」「意図的に失敗させた場合のエラー文言」「つまずきやすい点」を手順ログ（Markdown）に残す。ビルド・lint・テストが通ることを確認し、起動したサーバーは必ず止める。`document/` には触れない。
2. **執筆（writer）**: 手順ログと実プロジェクトの実物だけを材料に `document/<track>/<NN>-<slug>/index.html` を書く。コードと出力は創作せず、実ファイルを読み込んでエスケープ埋め込みする。ログに無い挙動は書かないか「要確認」の印を付ける。カリキュラム表・カタログ・前の項目の「次へ」も更新する。textlint（ja-technical-writing）で校正する。
3. **レビュー（reviewer）**: 教材のコード・diff・出力を実プロジェクトと突き合わせ、diff の変更前が前の項目の完成状態と一致するか、途中段階のコードがその時点で動くかを別コピーで再現して確かめる。「要確認」の箇所は実際に動かして確定させる。フレームワークの仕様説明は同梱ドキュメントや公式ドキュメントで裏取りする。指摘は「ファイル:行番号 — 問題 — 修正案」で返し、修正はしない。
4. **修正と PR**: 指摘を反映し（コードを直した場合は教材内のコードも一致させる）、項目ブランチにコミットして push する。`docs/<track>-<NN>-<slug>` を最新の `main` から切って `document/` だけを取り込み、PR を出す。マージ後に Obsidian の `progress.md` を更新する。

教材の出発点は「前の項目の完成コードをそのまま使う」とし、学習者にはブランチの取り込み手順を見せない。`package.json` の `name` など項目名に依存する値は出発点で直す。

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
