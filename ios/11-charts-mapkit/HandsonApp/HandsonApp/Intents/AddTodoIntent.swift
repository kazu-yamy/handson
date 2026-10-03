import AppIntents
import SwiftData
import WidgetKit

// ショートカットや Siri から ToDo を追加する
struct AddTodoIntent: AppIntent {
    static let title: LocalizedStringResource = "ToDo を追加"

    @Parameter(title: "タイトル")
    var todoTitle: String

    @MainActor
    func perform() async throws -> some IntentResult & ProvidesDialog {
        let container = try SharedStore.makeContainer()
        try addTodo(title: todoTitle, in: container.mainContext)
        WidgetCenter.shared.reloadAllTimelines()
        return .result(dialog: "「\(todoTitle)」を追加しました")
    }
}

// テストからも呼べるよう、保存処理は関数に分ける
func addTodo(title: String, in context: ModelContext) throws {
    context.insert(TodoRecord(title: title))
    try context.save()
}
