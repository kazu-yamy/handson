//
//  ContentView.swift
//  HandsonApp
//

import SwiftData
import SwiftUI
import WidgetKit

struct ContentView: View {
    // 保存・削除を行うための窓口。.modelContainer が環境に入れてくれる
    @Environment(\.modelContext) private var modelContext

    @State private var tapCount = 0
    @State private var showsDone = true
    @State private var newTitle = ""
    @State private var importMessage: String?
    private let client = TodoAPIClient()

    private var canAdd: Bool {
        (try? validate(name: newTitle)) != nil
    }

    var body: some View {
        NavigationStack {
            List {
                Section {
                    Button("タップ回数: \(tapCount)") {
                        tapCount += 1
                    }
                    .buttonStyle(.borderless)

                    Toggle("完了も表示", isOn: $showsDone)
                }

                TodoSection(showsDone: showsDone)

                Section {
                    Button("サーバーから取り込む") {
                        Task {
                            do {
                                let dtos = try await client.fetchTodos(limit: 5)
                                try importTodos(dtos, into: modelContext)
                                importMessage = "\(dtos.count) 件を取り込みました"
                            } catch {
                                importMessage = "取り込みに失敗しました"
                            }
                        }
                    }
                } footer: {
                    if let importMessage {
                        Text(importMessage)
                    }
                }

                Section("追加") {
                    HStack {
                        TextField("新しいタスク", text: $newTitle)
                        Button("追加") {
                            modelContext.insert(TodoRecord(title: newTitle))
                            // 自動保存を待たず、追加の直後に保存する
                            try? modelContext.save()
                            // ウィジェットの表示を更新してもらう
                            WidgetCenter.shared.reloadAllTimelines()
                            newTitle = ""
                        }
                        .buttonStyle(.borderless)
                        .disabled(!canAdd)
                    }
                }
            }
            .navigationDestination(for: TodoRecord.self) { item in
                TodoDetailView(record: item)
            }
            .navigationTitle("ToDo")
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    NavigationLink {
                        TodoStatsView()
                    } label: {
                        Image(systemName: "chart.bar")
                    }
                    .accessibilityLabel("統計")
                }
                ToolbarItem(placement: .topBarTrailing) {
                    EditButton()
                }
            }
        }
    }
}

#Preview {
    ContentView()
        // 保存せずメモリ上だけに作る。Preview のたびにまっさらな状態で始まる
        .modelContainer(for: TodoRecord.self, inMemory: true)
}
