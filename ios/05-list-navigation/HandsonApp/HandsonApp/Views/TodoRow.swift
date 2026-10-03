import SwiftUI

struct TodoRow: View {
    @Binding var item: TodoItem

    var body: some View {
        HStack(spacing: 12) {
            // 完了の切り替えはアイコンのボタンだけに任せる（行のタップは NavigationLink に譲る）
            Button {
                item.toggle()
            } label: {
                // switch: Status の case ごとにアイコンを切り替える
                switch item.status {
                case .todo:
                    Image(systemName: "circle")
                        .foregroundStyle(.secondary)
                case .done:
                    Image(systemName: "checkmark.circle.fill")
                        .foregroundStyle(.green)
                }
            }
            .buttonStyle(.borderless)
            .accessibilityLabel(item.isDone ? "未完了に戻す" : "完了にする")

            Text(item.title)
                .strikethrough(item.isDone)

            Spacer()

            Text(item.priority.label)
                .font(.caption)
                .padding(.horizontal, 8)
                .padding(.vertical, 2)
                .background(.quaternary, in: Capsule())
        }
    }
}

#Preview("未完了") {
    @Previewable @State var item = TodoItem(title: "牛乳を買う", priority: .high)
    TodoRow(item: $item)
        .padding()
}

#Preview("完了") {
    @Previewable @State var item = TodoItem(title: "メールを返す", priority: .low, status: .done(Date()))
    TodoRow(item: $item)
        .padding()
}
