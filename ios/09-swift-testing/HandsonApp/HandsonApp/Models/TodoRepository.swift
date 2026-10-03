import Foundation

enum RepositoryError: Error {
    case failed
}

struct TodoRepository {
    var delay: Duration = .seconds(1)
    var shouldFail = false
    // nil のときは 06 までの固定データ（Preview やテスト用）。アプリ本体は TodoAPIClient を渡す
    var client: TodoAPIClient?
    let cache = TodoCache()

    func fetchAll(useCache: Bool = true) async throws -> [TodoItem] {
        if useCache, let cached = await cache.value(for: "items") {
            return cached
        }
        let items: [TodoItem]
        if let client {
            // 本物の通信。API の 1 件ずつを画面用の TodoItem に変換する
            items = try await client.fetchTodos(limit: 5).map(TodoItem.init(dto:))
        } else {
            try await Task.sleep(for: delay)
            if shouldFail { throw RepositoryError.failed }
            items = [
                TodoItem(title: "牛乳を買う", priority: .high),
                TodoItem(title: "メールを返す", priority: .low, status: .done(Date())),
                TodoItem(title: "SwiftUI を勉強する"),
            ]
        }
        await cache.store(items, for: "items")
        return items
    }

    // サーバーに作成を依頼し、返ってきた 1 件を画面用のモデルにして返す
    func create(title: String, priority: Priority = .medium) async throws -> TodoItem {
        guard let client else {
            return TodoItem(title: title, priority: priority)
        }
        var item = TodoItem(dto: try await client.createTodo(title: title))
        item.priority = priority
        return item
    }

    func fetchProfile() async throws -> Profile {
        try await Task.sleep(for: delay)
        return Profile(name: "Taro Yamada", nickname: "Taro")
    }
}
