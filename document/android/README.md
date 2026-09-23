# Android トラック

Kotlin + Jetpack Compose を使った Android アプリ開発を学ぶトラックです。サードパーティライブラリ（Retrofit 等）は使わず、公式パッケージのみで構成します。

## 前提バージョン

- Kotlin 2.4.20
- AGP（Android Gradle Plugin）9.4
- Android Studio 最新安定版
- Compose BOM 2026.09.00
- Material3 1.4
- targetSdk 36 / compileSdk 37 / minSdk 26

## 扱う公式パッケージ

- Compose UI / Foundation
- Compose Material3（+ Adaptive）
- Navigation 3
- Lifecycle ViewModel + kotlinx.coroutines / Flow
- Room 3（`androidx.room3`、KSP）
- DataStore
- Hilt
- WorkManager
- Ktor Client + kotlinx.serialization
- CameraX / Paging 3（発展編）

## カリキュラム表

| 番号 | タイトル | 到達目標 | 所要時間 | 状態 |
| --- | --- | --- | --- | --- |
| 01 | 環境構築（Android Studio・Version Catalog） | Android Studio と Version Catalog を用いたプロジェクトを作成できる | 45分 | 準備中 |
| 02 | Kotlin 基礎 | Android 開発に必要な Kotlin の文法（null 安全・データクラス・拡張関数）を理解する | 45分 | 準備中 |
| 03 | Compose 入門 | Composable 関数の基本とプレビューを使った UI 構築ができる | 45分 | 準備中 |
| 04 | 状態と State hoisting | `remember` / `mutableStateOf` と State hoisting パターンを実装できる | 45分 | 準備中 |
| 05 | Material3 と LazyColumn | Material3 コンポーネントと `LazyColumn` でリスト UI を構築できる | 45分 | 準備中 |
| 06 | ViewModel と StateFlow | ViewModel と `StateFlow` を使った状態管理を実装できる | 45分 | 準備中 |
| 07 | Navigation 3 | Navigation 3 で画面遷移とバックスタック管理を実装できる | 45分 | 準備中 |
| 08 | Ktor と serialization で API 取得 | Ktor Client と kotlinx.serialization を使った API 通信を実装できる | 45分 | 準備中 |
| 09 | Room 3 と DataStore | Room 3 でのローカル DB 操作と DataStore での設定保存を実装できる | 45分 | 準備中 |
| 10 | Hilt | Hilt を使った依存性注入を実装できる | 45分 | 準備中 |
| 11 | WorkManager | WorkManager を使ったバックグラウンド処理を実装できる | 45分 | 準備中 |
| 12 | テストとリリースビルド | ユニットテストを書き、リリースビルドを作成できる | 45分 | 準備中 |
