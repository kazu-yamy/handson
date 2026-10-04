# Android トラック

Kotlin + Jetpack Compose を使った Android アプリ開発を学ぶトラックです。サードパーティライブラリ（Retrofit 等）は使わず、公式パッケージのみで構成します。

## 前提バージョン

- Kotlin 2.4.20
- AGP（Android Gradle Plugin）9.4
- Android Studio 最新安定版
- Compose BOM 2026.09.00
- Material3 1.4
- targetSdk 37 / compileSdk 37 / minSdk 26

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

進捗は Obsidian の Projects/handson/progress.md で管理する。

| 番号 | タイトル | 到達目標 | 所要時間 |
| --- | --- | --- | --- |
| 01 | [環境構築（Android Studio・Version Catalog）](./01-setup/index.html) | Android Studio と Version Catalog を用いたプロジェクトを作成できる | 45分 |
| 02 | [Kotlin 基礎](./02-kotlin-basics/index.html) | Android 開発に必要な Kotlin の文法（null 安全・データクラス・拡張関数）を理解する | 45分 |
| 03 | [Compose 入門](./03-compose-basics/index.html) | Composable 関数の基本とプレビューを使った UI 構築ができる | 45分 |
| 04 | [状態と State hoisting](./04-state/index.html) | `remember` / `mutableStateOf` と State hoisting パターンを実装できる | 45分 |
| 05 | [Material3 と LazyColumn](./05-material3-lazycolumn/index.html) | `LazyColumn` で一覧を表示し、`Scaffold` のスロットでスクロール位置に応じたボタンと、削除の取り消し付き Snackbar を実装できる | 45分 |
| 06 | [Material3 の画面部品](./06-material3-components/index.html) | `SwipeToDismissBox` のスワイプ削除、`stickyHeader` と `FilterChip` のセクション分けと絞り込み、`ModalBottomSheet` と `AlertDialog` で一覧の操作を作れる | 60分 |
| 07 | [ViewModel と StateFlow](./07-viewmodel-stateflow/index.html) | 一覧の状態を ViewModel と `StateFlow` に移し、`SavedStateHandle` でプロセスの終了から復元し、`viewModelScope` の非同期処理を単体テストできる | 60分 |
| 08 | [Navigation 3](./08-navigation3/index.html) | キーとバックスタックと `NavDisplay` で一覧と詳細の画面遷移を作り、`rememberNavBackStack` で回転とプロセスの再生成の後も開いていた画面を復元できる | 60分 |
| 09 | [Ktor と serialization で API 取得](./09-ktor-serialization/index.html) | Ktor Client と kotlinx.serialization で API から一覧を取得し、読み込み中・成功・失敗（再試行）を表示できる | 60分 |
| 10 | [MockEngine でネットワークのテスト](./10-mockengine-testing/index.html) | `MockEngine` で、API の成功と失敗・ViewModel の失敗と再試行とキャンセル・失敗表示を、ネットワークに出ずにテストできる | 60分 |
| 11 | [Room 3 でローカル保存](./11-room/index.html) | Room 3 の Entity・DAO・Database と Repository で、一覧の追加・完了・削除を端末の DB に保存し、アプリを終了しても残るようにできる | 60分 |
| 12 | [DataStore で設定を保存](./12-datastore/index.html) | Preferences DataStore に「最初の一覧を取得済み」の印と絞り込みを保存し、全件削除しても API から取り直さず、アプリを再起動しても絞り込みが保たれるようにできる | 60分 |
| 13 | [Hilt で依存性注入](./13-hilt/index.html) | `AppContainer` を Hilt の module・`@Inject` constructor・`@HiltViewModel` に置き換え、UI テストの偽物を `@TestInstallIn` で差し込める | 60分 |
| 14 | [WorkManager で定期バックアップ](./14-workmanager/index.html) | `@HiltWorker` の Worker を `Configuration.Provider` で動かし、条件・リトライ付きの定期バックアップを `enqueueUniquePeriodicWork` で予約して、adb とテストで実行を確かめられる | 60分 |
| 15 | [テストの仕上げ](./15-testing/index.html) | カバレッジで、偽物に置き換えて測れていない所を見つけ、本物の Room の DAO（偽物との契約テスト）と DataStore のファイルのテスト、`@UninstallModules`・`@BindValue`・`hiltRule.inject()` で Hilt が組み立てた Worker のテストを書ける | 60分 |
| 16 | リリースビルドと署名 | R8 の出力と `retrace` で release の APK を読み解き、`versionCode` を上げて自分の鍵で署名した APK で更新でき、AAB の中身を確かめられる | 60分 |
