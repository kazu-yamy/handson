//
//  TodoWidget.swift
//  TodoWidget
//

import AppIntents
import SwiftData
import SwiftUI
import WidgetKit

struct TodoEntry: TimelineEntry {
    let date: Date
    let snapshot: TodoSnapshot
}

struct Provider: TimelineProvider {
    func placeholder(in context: Context) -> TodoEntry {
        loadEntry()
    }

    func getSnapshot(in context: Context, completion: @escaping (TodoEntry) -> Void) {
        completion(loadEntry())
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<TodoEntry>) -> Void) {
        // 更新はアプリ側から依頼する（.never）
        completion(Timeline(entries: [loadEntry()], policy: .never))
    }

    // ウィジェットはアプリとは別のプロセス。App Group のストアを開いて読む
    private func loadEntry() -> TodoEntry {
        do {
            let container = try SharedStore.makeContainer()
            let snapshot = try SharedStore.snapshot(in: ModelContext(container))
            return TodoEntry(date: .now, snapshot: snapshot)
        } catch {
            return TodoEntry(date: .now, snapshot: .empty)
        }
    }
}

struct TodoWidgetEntryView: View {
    var entry: TodoEntry

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text("残り \(entry.snapshot.remaining) 件")
                .font(.headline)
            ForEach(entry.snapshot.titles.indices, id: \.self) { index in
                Text(entry.snapshot.titles[index])
                    .font(.caption)
                    .lineLimit(1)
            }
            if !entry.snapshot.titles.isEmpty {
                // タップすると、ウィジェットのプロセスで Intent が実行される
                Button(intent: CompleteLatestTodoIntent()) {
                    Label("完了", systemImage: "checkmark.circle")
                }
                .font(.caption)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

struct TodoWidget: Widget {
    let kind: String = "TodoWidget"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: Provider()) { entry in
            TodoWidgetEntryView(entry: entry)
                .containerBackground(.fill.tertiary, for: .widget)
        }
        .configurationDisplayName("ToDo")
        .description("未完了の ToDo を表示します。")
        .supportedFamilies([.systemSmall, .systemMedium])
    }
}

#Preview(as: .systemSmall) {
    TodoWidget()
} timeline: {
    TodoEntry(date: .now, snapshot: TodoSnapshot(remaining: 2, titles: ["牛乳を買う", "本を返す"]))
}
