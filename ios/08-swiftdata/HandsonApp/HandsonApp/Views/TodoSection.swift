import SwiftData
import SwiftUI

// Todo の一覧セクション。@Query の条件は init で組み立てる
// （@Query の filter は View の State を直接参照できないため、子 View に引数として渡す）
struct TodoSection: View {
    @Query private var items: [TodoRecord]
    @Environment(\.modelContext) private var modelContext

    init(showsDone: Bool) {
        // 「完了も表示」がオフなら未完了だけに絞る。どちらも作成日時の古い順
        let predicate = #Predicate<TodoRecord> { showsDone || !$0.isDone }
        _items = Query(filter: predicate, sort: \.createdAt)
    }

    var body: some View {
        Section("Todo") {
            if items.isEmpty {
                ContentUnavailableView("ToDo はまだありません", systemImage: "checklist")
            }
            ForEach(items) { item in
                NavigationLink(value: item) {
                    TodoRow(record: item)
                }
            }
            .onDelete { offsets in
                for index in offsets {
                    modelContext.delete(items[index])
                }
                // 自動保存を待たず、削除の直後に保存する
                try? modelContext.save()
            }
        }
    }
}
