import Foundation
import SwiftData

// アプリとウィジェットで同じデータベースを開くための設定
nonisolated enum SharedStore {
    static let appGroupID = "group.com.example.HandsonApp"

    static func makeContainer(inMemory: Bool = false) throws -> ModelContainer {
        let configuration = inMemory
            ? ModelConfiguration(isStoredInMemoryOnly: true)
            : ModelConfiguration(groupContainer: .identifier(appGroupID))
        return try ModelContainer(for: TodoRecord.self, configurations: configuration)
    }
}

// ウィジェットに表示する内容
nonisolated struct TodoSnapshot: Equatable, Sendable {
    let remaining: Int
    let titles: [String]

    static let empty = TodoSnapshot(remaining: 0, titles: [])
}

extension SharedStore {
    // 未完了の件数と、新しい順の先頭 limit 件のタイトル
    static func snapshot(in context: ModelContext, limit: Int = 3) throws -> TodoSnapshot {
        var descriptor = FetchDescriptor<TodoRecord>(
            predicate: #Predicate { !$0.isDone },
            sortBy: [SortDescriptor(\.createdAt, order: .reverse)]
        )
        let remaining = try context.fetchCount(descriptor)
        descriptor.fetchLimit = limit
        let titles = try context.fetch(descriptor).map(\.title)
        return TodoSnapshot(remaining: remaining, titles: titles)
    }
}
