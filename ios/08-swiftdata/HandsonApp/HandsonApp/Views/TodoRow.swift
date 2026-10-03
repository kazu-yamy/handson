import SwiftData
import SwiftUI

struct TodoRow: View {
    // @Model のオブジェクトはクラスなので、@Binding ではなく @Bindable で受け取る
    @Bindable var record: TodoRecord
    @Environment(\.modelContext) private var modelContext

    var body: some View {
        HStack(spacing: 12) {
            // 完了の切り替えはアイコンのボタンだけに任せる（行のタップは NavigationLink に譲る）
            Button {
                record.toggle()
                try? modelContext.save()
            } label: {
                if record.isDone {
                    Image(systemName: "checkmark.circle.fill")
                        .foregroundStyle(.green)
                } else {
                    Image(systemName: "circle")
                        .foregroundStyle(.secondary)
                }
            }
            .buttonStyle(.borderless)
            .accessibilityLabel(record.isDone ? "未完了に戻す" : "完了にする")

            Text(record.title)
                .strikethrough(record.isDone)

            Spacer()

            Text(record.priority.label)
                .font(.caption)
                .padding(.horizontal, 8)
                .padding(.vertical, 2)
                .background(.quaternary, in: Capsule())
        }
    }
}
