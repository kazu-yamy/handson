//
//  HandsonAppApp.swift
//  HandsonApp
//

import SwiftData
import SwiftUI

@main
struct HandsonAppApp: App {
    private let container: ModelContainer

    init() {
        // UI テストは起動引数 -uiTesting を付けて起動する。
        // そのときはメモリ上だけのコンテナを使い、テストごとにまっさらな状態で始める
        let isUITesting = ProcessInfo.processInfo.arguments.contains("-uiTesting")
        let configuration = ModelConfiguration(isStoredInMemoryOnly: isUITesting)
        do {
            container = try ModelContainer(for: TodoRecord.self, configurations: configuration)
        } catch {
            fatalError("ModelContainer を作れませんでした: \(error)")
        }
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
        .modelContainer(container)
    }
}
