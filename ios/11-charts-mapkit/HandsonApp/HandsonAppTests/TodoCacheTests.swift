import Foundation
import Testing
@testable import HandsonApp

struct TodoCacheTests {

    @Test func storeAndReadValue() async {
        let cache = TodoCache()
        #expect(await cache.value(for: "items") == nil)

        await cache.store([TodoItem(title: "買い物")], for: "items")
        let cached = await cache.value(for: "items")
        #expect(cached?.map(\.title) == ["買い物"])
    }

    @Test func concurrentWritesAreSerialized() async {
        let cache = TodoCache()
        await withTaskGroup(of: Void.self) { group in
            for i in 0..<100 {
                group.addTask {
                    await cache.store([TodoItem(title: "item \(i)")], for: "key \(i)")
                }
            }
        }
        for i in 0..<100 {
            #expect(await cache.value(for: "key \(i)") != nil)
        }
    }

    @Test(.tags(.slow), .timeLimit(.minutes(1)))
    func repositoryUsesCacheOnSecondFetch() async throws {
        let repository = TodoRepository(delay: .milliseconds(500))
        let clock = ContinuousClock()

        let first = clock.now
        _ = try await repository.fetchAll()
        #expect(clock.now - first >= .milliseconds(500))

        let second = clock.now
        _ = try await repository.fetchAll()
        #expect(clock.now - second < .milliseconds(200))  // キャッシュから即座に返る

        let third = clock.now
        _ = try await repository.fetchAll(useCache: false)
        #expect(clock.now - third >= .milliseconds(500))  // 強制再取得は待つ
    }
}
