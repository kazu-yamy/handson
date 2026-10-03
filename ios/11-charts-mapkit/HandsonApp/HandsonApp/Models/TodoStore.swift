import Foundation
import Observation
import SwiftUI

@Observable
final class TodoStore {
    var items: [TodoItem]
    var isLoading = false
    var profile: Profile?
    var summary: TodoSummary?
    var errorMessage: String?

    private let repository: TodoRepository

    init(items: [TodoItem] = [], repository: TodoRepository = TodoRepository()) {
        self.items = items
        self.repository = repository
    }

    func load(useCache: Bool = true) async {
        isLoading = true
        errorMessage = nil
        defer { isLoading = false }
        do {
            // 2 つの取得を同時に始める。待つのはここ（await）
            async let fetchedItems = repository.fetchAll(useCache: useCache)
            async let fetchedProfile = repository.fetchProfile()
            items = try await fetchedItems
            profile = try await fetchedProfile
            summary = await TodoSummarizer().summarize(items)
        } catch is CancellationError {
            // キャンセルはエラーではない。画面は前の状態のままにする
        } catch {
            errorMessage = "読み込みに失敗しました"
        }
    }

    func add(title: String, priority: Priority = .medium) async {
        errorMessage = nil
        do {
            // JSONPlaceholder は保存しないので、返ってきた 1 件をローカルの一覧に足す
            items.append(try await repository.create(title: title, priority: priority))
        } catch {
            errorMessage = "追加に失敗しました"
        }
    }

    func toggle(_ item: TodoItem) {
        guard let index = items.firstIndex(where: { $0.title == item.title }) else { return }
        items[index].toggle()
    }

    func remove(at offsets: IndexSet) {
        items.remove(atOffsets: offsets)
    }

    // 完了済みの項目をすべて削除する。削除した項目ごとに onRemove を呼ぶ
    func removeCompleted(onRemove: (TodoItem) -> Void = { _ in }) {
        let completed = items.filter(\.isDone)
        items.removeAll(where: \.isDone)
        completed.forEach(onRemove)
    }

    func move(from source: IndexSet, to destination: Int) {
        items.move(fromOffsets: source, toOffset: destination)
    }

    static let sample = TodoStore(items: [
        TodoItem(title: "牛乳を買う", priority: .high),
        TodoItem(title: "メールを返す", priority: .low, status: .done(Date())),
        TodoItem(title: "SwiftUI を勉強する"),
    ])
}
