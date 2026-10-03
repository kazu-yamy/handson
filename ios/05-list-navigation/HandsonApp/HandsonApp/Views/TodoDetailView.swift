import SwiftUI

struct TodoDetailView: View {
    let item: TodoItem

    var body: some View {
        List {
            LabeledContent("タイトル", value: item.title)
            LabeledContent("優先度", value: item.priority.label)
            LabeledContent("状態", value: item.isDone ? "完了" : "未完了")
            if case .done(let date) = item.status {
                LabeledContent("完了日時") {
                    Text(date, format: .dateTime.year().month().day().hour().minute())
                }
            }
        }
        .navigationTitle(item.title)
        .navigationBarTitleDisplayMode(.inline)
    }
}

#Preview {
    NavigationStack {
        TodoDetailView(item: TodoItem(title: "メールを返す", priority: .low, status: .done(Date())))
    }
}
