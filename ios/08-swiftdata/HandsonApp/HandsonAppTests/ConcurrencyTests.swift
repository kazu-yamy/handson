import Foundation
import Testing
@testable import HandsonApp

@MainActor
struct ConcurrencyTests {

    @Test func summarizerRunsOffTheMainThread() async {
        // テスト関数は @MainActor。ここはメインスレッド
        MainActor.assertIsolated()
        #expect(runningOnMainThread())

        let items = [
            TodoItem(title: "a", status: .done(Date())),
            TodoItem(title: "b"),
        ]
        let summary = await TodoSummarizer().summarize(items)

        // @concurrent の関数はメインアクターの外で動く
        #expect(!summary.ranOnMainThread)
        #expect(summary.text == "1 / 2 件完了")

        // await から戻ると再びメインアクター
        MainActor.assertIsolated()
        #expect(runningOnMainThread())
    }

    @Test func sequentialFetchTakesTwoSeconds() async throws {
        let repository = TodoRepository()
        let clock = ContinuousClock()
        let start = clock.now
        _ = try await repository.fetchAll()
        _ = try await repository.fetchProfile()
        let elapsed = clock.now - start
        print("DEBUG sequential:", elapsed)
        #expect(elapsed >= .seconds(2))
    }

    @Test func concurrentFetchTakesOneSecond() async throws {
        let repository = TodoRepository()
        let clock = ContinuousClock()
        let start = clock.now
        async let items = repository.fetchAll()
        async let profile = repository.fetchProfile()
        _ = try await (items, profile)
        let elapsed = clock.now - start
        print("DEBUG concurrent:", elapsed)
        #expect(elapsed >= .seconds(1))
        #expect(elapsed < .milliseconds(1800))
    }

    @Test func loadFillsItemsAndSummary() async {
        let store = TodoStore()
        #expect(store.items.isEmpty)
        #expect(store.profile == nil)
        await store.load()
        #expect(store.items.count == 3)
        #expect(store.profile?.displayName == "Taro")
        #expect(store.summary?.text == "1 / 3 件完了")
        #expect(!store.isLoading)
    }

    @Test func sleepThrowsCancellationErrorWhenCancelled() async {
        let clock = ContinuousClock()
        let start = clock.now
        let task = Task {
            try await Task.sleep(for: .seconds(10))
        }
        task.cancel()
        await #expect(throws: CancellationError.self) {
            try await task.value
        }
        // 10 秒待たずにすぐ終わる
        #expect(clock.now - start < .seconds(2))
    }

    @Test func cancelledLoadStopsEarlyAndResetsLoading() async throws {
        let store = TodoStore(repository: TodoRepository(delay: .seconds(5)))
        let task = Task { await store.load() }

        try await Task.sleep(for: .milliseconds(200))
        #expect(store.isLoading)

        let clock = ContinuousClock()
        let start = clock.now
        task.cancel()
        await task.value

        #expect(clock.now - start < .seconds(2))  // 5 秒待たずに終わる
        #expect(!store.isLoading)
        #expect(store.items.isEmpty)
        #expect(store.errorMessage == nil)  // キャンセルはエラー扱いにしない
    }

    @Test func failedLoadSetsErrorMessage() async {
        let store = TodoStore(repository: TodoRepository(delay: .milliseconds(10), shouldFail: true))
        await store.load()
        #expect(store.errorMessage == "読み込みに失敗しました")
        #expect(store.items.isEmpty)
        #expect(!store.isLoading)
    }
}
