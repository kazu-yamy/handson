# iOS トラック

SwiftUI + Swift Concurrency を使った iOS アプリ開発を学ぶトラックです。UIKit / Core Data / Combine は旧世代の技術として扱い、公式の最新フレームワークのみで構成します。

## 前提バージョン

- Xcode 27（macOS 26.6 以上）
- Swift 6.4（Swift 6 言語モード）
- 最小対象 iOS 18

## 扱う公式フレームワーク・パッケージ

- SwiftUI
- Observation（`@Observable`）
- Swift Concurrency（async/await、Actor）
- SwiftData
- URLSession + Codable
- Swift Testing（UI テストは XCTest）
- WidgetKit
- App Intents
- Swift Charts / MapKit
- StoreKit 2
- 公式 SPM パッケージ: swift-collections、swift-algorithms、swift-async-algorithms、swift-openapi-generator、swift-log

## カリキュラム表

進捗は Obsidian の Projects/handson/progress.md で管理する。

| 番号 | タイトル | 到達目標 | 所要時間 |
| --- | --- | --- | --- |
| 01 | 環境構築（Xcode 27・Swift 6 言語モード） | Xcode 27 で Swift 6 言語モードのプロジェクトを作成できる | 45分 |
| 02 | Swift 基礎 | iOS 開発に必要な Swift の文法（オプショナル・構造体・プロトコル）を理解する | 45分 |
| 03 | SwiftUI 入門 | View の基本とプレビューを使った UI 構築ができる | 45分 |
| 04 | @State・@Binding・@Observable | 状態管理の基本パターンを実装できる | 45分 |
| 05 | List と NavigationStack | `List` と `NavigationStack` で一覧・詳細画面の遷移を実装できる | 45分 |
| 06 | async/await と @MainActor | Swift Concurrency を使った非同期処理と `@MainActor` の使い分けができる | 45分 |
| 07 | URLSession と Codable | URLSession と Codable を使った API 通信を実装できる | 45分 |
| 08 | SwiftData | SwiftData を使ったローカルデータの永続化を実装できる | 45分 |
| 09 | Swift Testing | Swift Testing を使ったユニットテストを書ける | 45分 |
| 10 | WidgetKit と App Intents | ウィジェットと App Intents を使ったショートカット連携を実装できる | 45分 |
| 11 | Charts と MapKit | Swift Charts でのグラフ表示と MapKit での地図表示を実装できる | 45分 |
| 12 | TestFlight で配布 | TestFlight を使ったベータ配布の手順を実施できる | 45分 |
