import Foundation
import SwiftData
import Testing
@testable import HandsonApp

@MainActor
struct TodoRecordTests {
    // テストごとにメモリ上だけのコンテナを作る。ファイルを作らないので互いに影響しない
    // ModelContext はコンテナを強参照しない。コンテナも返して、テストの間は呼び出し側が保持する
    private func makeContainer() throws -> ModelContainer {
        let configuration = ModelConfiguration(isStoredInMemoryOnly: true)
        return try ModelContainer(for: TodoRecord.self, configurations: configuration)
    }

    @Test func insertして取得できる() throws {
        let container = try makeContainer()
        let context = container.mainContext
        context.insert(TodoRecord(title: "牛乳を買う"))
        try context.save()

        let records = try context.fetch(FetchDescriptor<TodoRecord>())
        #expect(records.count == 1)
        #expect(records.first?.title == "牛乳を買う")
    }

    @Test func deleteすると取得できなくなる() throws {
        let container = try makeContainer()
        let context = container.mainContext
        let record = TodoRecord(title: "メールを返す")
        context.insert(record)
        try context.save()

        context.delete(record)
        try context.save()

        #expect(try context.fetch(FetchDescriptor<TodoRecord>()).isEmpty)
    }

    @Test func Predicateで未完了だけに絞れる() throws {
        let container = try makeContainer()
        let context = container.mainContext
        let done = TodoRecord(title: "完了済み")
        done.toggle()
        context.insert(done)
        context.insert(TodoRecord(title: "未完了"))

        let descriptor = FetchDescriptor<TodoRecord>(predicate: #Predicate { !$0.isDone })
        let records = try context.fetch(descriptor)
        #expect(records.map(\.title) == ["未完了"])
    }

    @Test func sortByで作成日時の古い順に並ぶ() throws {
        let container = try makeContainer()
        let context = container.mainContext
        let now = Date.now
        context.insert(TodoRecord(title: "新しい", createdAt: now))
        context.insert(TodoRecord(title: "古い", createdAt: now.addingTimeInterval(-60)))

        let descriptor = FetchDescriptor<TodoRecord>(sortBy: [SortDescriptor(\.createdAt)])
        let records = try context.fetch(descriptor)
        #expect(records.map(\.title) == ["古い", "新しい"])
    }

    @Test func toggleで完了日時が入る() {
        let record = TodoRecord(title: "x")
        record.toggle()
        #expect(record.isDone)
        #expect(record.completedAt != nil)
        record.toggle()
        #expect(record.completedAt == nil)
    }
}

@MainActor
struct TodoImporterTests {
    @Test func 取り込みを2回繰り返しても重複しない() throws {
        let configuration = ModelConfiguration(isStoredInMemoryOnly: true)
        let container = try ModelContainer(for: TodoRecord.self, configurations: configuration)
        let context = container.mainContext
        let dtos = [
            TodoDTO(userId: 1, id: 1, title: "A", completed: false),
            TodoDTO(userId: 1, id: 2, title: "B", completed: true),
        ]

        try importTodos(dtos, into: context)
        try importTodos(dtos, into: context)

        let records = try context.fetch(FetchDescriptor<TodoRecord>())
        #expect(records.count == 2)
        #expect(records.filter(\.isDone).count == 1)
    }
}
