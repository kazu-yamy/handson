//
//  ContentView.swift
//  HandsonApp
//

import SwiftUI

struct ContentView: View {
    @Environment(TodoStore.self) private var store

    @State private var tapCount = 0
    @State private var showsDone = true
    @State private var newTitle = ""

    private var canAdd: Bool {
        (try? validate(name: newTitle)) != nil
    }

    var body: some View {
        @Bindable var store = store
        NavigationStack {
            List {
                Section {
                    if let profile = store.profile {
                        ProfileHeader(profile: profile)
                    }

                    Button("タップ回数: \(tapCount)") {
                        tapCount += 1
                    }
                    .buttonStyle(.borderless)

                    Toggle("完了も表示", isOn: $showsDone)
                }

                if let message = store.errorMessage {
                    Section {
                        Text(message)
                            .foregroundStyle(.red)
                    }
                }

                Section {
                    if store.isLoading {
                        ProgressView("読み込み中…")
                    }
                    ForEach($store.items) { $item in
                        if showsDone || !item.isDone {
                            NavigationLink(value: item) {
                                TodoRow(item: $item)
                            }
                        }
                    }
                    .onDelete { offsets in
                        store.remove(at: offsets)
                    }
                    .onMove { source, destination in
                        store.move(from: source, to: destination)
                    }
                } header: {
                    Text("Todo")
                } footer: {
                    if let summary = store.summary {
                        Text(summary.text)
                    }
                }

                Section("追加") {
                    HStack {
                        TextField("新しいタスク", text: $newTitle)
                        Button("追加") {
                            let title = newTitle
                            newTitle = ""
                            Task {
                                await store.add(title: title)
                            }
                        }
                        .buttonStyle(.borderless)
                        .disabled(!canAdd)
                    }
                }
            }
            .navigationDestination(for: TodoItem.self) { item in
                TodoDetailView(item: item)
            }
            .task {
                await store.load()
            }
            .navigationTitle("ToDo")
            .refreshable {
                await store.load(useCache: false)
            }
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button("再読み込み") {
                        Task {
                            await store.load(useCache: false)
                        }
                    }
                    .disabled(store.isLoading)
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
        .environment(TodoStore.sample)
}
