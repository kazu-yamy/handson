import Foundation

enum RepositoryError: Error {
    case failed
}

struct TodoRepository {
    var delay: Duration = .seconds(1)
    var shouldFail = false
    let cache = TodoCache()

    // ネットワークの代わりに 1 秒待って固定のデータを返す
    func fetchAll(useCache: Bool = true) async throws -> [TodoItem] {
        if useCache, let cached = await cache.value(for: "items") {
            return cached
        }
        try await Task.sleep(for: delay)
        if shouldFail { throw RepositoryError.failed }
        let items = [
            TodoItem(title: "牛乳を買う", priority: .high),
            TodoItem(title: "メールを返す", priority: .low, status: .done(Date())),
            TodoItem(title: "SwiftUI を勉強する"),
        ]
        await cache.store(items, for: "items")
        return items
    }

    func fetchProfile() async throws -> Profile {
        try await Task.sleep(for: delay)
        return Profile(name: "Taro Yamada", nickname: "Taro")
    }
}
