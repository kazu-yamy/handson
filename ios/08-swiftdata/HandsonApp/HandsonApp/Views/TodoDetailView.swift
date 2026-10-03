import SwiftUI

struct TodoDetailView: View {
    let record: TodoRecord

    var body: some View {
        List {
            LabeledContent("タイトル", value: record.title)
            LabeledContent("優先度", value: record.priority.label)
            LabeledContent("状態", value: record.isDone ? "完了" : "未完了")
            if let date = record.completedAt {
                LabeledContent("完了日時") {
                    Text(date, format: .dateTime.year().month().day().hour().minute())
                }
            }
        }
        .navigationTitle(record.title)
        .navigationBarTitleDisplayMode(.inline)
    }
}
