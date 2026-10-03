import AppIntents
import SwiftData
import WidgetKit

// ウィジェットのボタンから、いちばん新しい未完了の ToDo を完了にする。
// ウィジェット拡張のプロセスで実行されるので、このファイルは両方のターゲットに入れる
struct CompleteLatestTodoIntent: AppIntent {
    static let title: LocalizedStringResource = "最新の ToDo を完了"

    @MainActor
    func perform() async throws -> some IntentResult {
        let container = try SharedStore.makeContainer()
        try completeLatest(in: container.mainContext)
        WidgetCenter.shared.reloadAllTimelines()
        return .result()
    }
}

@MainActor
func completeLatest(in context: ModelContext) throws {
    var descriptor = FetchDescriptor<TodoRecord>(
        predicate: #Predicate { !$0.isDone },
        sortBy: [SortDescriptor(\.createdAt, order: .reverse)]
    )
    descriptor.fetchLimit = 1
    try context.fetch(descriptor).first?.toggle()
    try context.save()
}
