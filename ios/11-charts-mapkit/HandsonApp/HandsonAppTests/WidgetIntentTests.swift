import Foundation
import SwiftData
import Testing
@testable import HandsonApp

@MainActor
@Suite("ウィジェットと App Intents")
struct WidgetIntentTests {
    // テストごとにメモリ上のコンテナを作る（App Group のストアには触れない）
    let container: ModelContainer

    init() throws {
        container = try SharedStore.makeContainer(inMemory: true)
    }

    @Test func スナップショットは未完了だけを数える() throws {
        let context = container.mainContext
        let done = TodoRecord(title: "済み", createdAt: .now.addingTimeInterval(-30))
        done.toggle()
        context.insert(done)
        context.insert(TodoRecord(title: "古い", createdAt: .now.addingTimeInterval(-20)))
        context.insert(TodoRecord(title: "新しい", createdAt: .now.addingTimeInterval(-10)))

        let snapshot = try SharedStore.snapshot(in: context, limit: 1)
        #expect(snapshot == TodoSnapshot(remaining: 2, titles: ["新しい"]))
    }

    @Test func addTodoで1件増える() throws {
        try addTodo(title: "牛乳を買う", in: container.mainContext)
        let titles = try container.mainContext.fetch(FetchDescriptor<TodoRecord>()).map(\.title)
        #expect(titles == ["牛乳を買う"])
    }

    @Test func completeLatestは最新の未完了を完了にする() throws {
        let context = container.mainContext
        context.insert(TodoRecord(title: "古い", createdAt: .now.addingTimeInterval(-20)))
        context.insert(TodoRecord(title: "新しい", createdAt: .now.addingTimeInterval(-10)))
        try completeLatest(in: context)
        #expect(try SharedStore.snapshot(in: context).titles == ["古い"])
    }
}
