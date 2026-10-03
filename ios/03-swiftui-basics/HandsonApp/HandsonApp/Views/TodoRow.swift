import SwiftUI

struct TodoRow: View {
    let item: TodoItem

    var body: some View {
        HStack(spacing: 12) {
            // switch: Status の case ごとにアイコンを切り替える
            switch item.status {
            case .todo:
                Image(systemName: "circle")
                    .foregroundStyle(.secondary)
            case .done:
                Image(systemName: "checkmark.circle.fill")
                    .foregroundStyle(.green)
            }

            Text(item.title)
                .strikethrough(isDone)

            Spacer()

            Text(item.priority.label)
                .font(.caption)
                .padding(.horizontal, 8)
                .padding(.vertical, 2)
                .background(.quaternary, in: Capsule())
        }
    }

    private var isDone: Bool {
        if case .done = item.status {
            return true
        }
        return false
    }
}

#Preview("未完了") {
    TodoRow(item: TodoItem(title: "牛乳を買う", priority: .high))
        .padding()
}

#Preview("完了") {
    TodoRow(item: TodoItem(title: "メールを返す", priority: .low, status: .done(Date())))
        .padding()
}
