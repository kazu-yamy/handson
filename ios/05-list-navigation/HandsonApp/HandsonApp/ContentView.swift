//
//  ContentView.swift
//  HandsonApp
//

import SwiftUI

struct ContentView: View {
    let profile = Profile(name: "Taro Yamada", nickname: "Taro")

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
                    ProfileHeader(profile: profile)

                    Button("タップ回数: \(tapCount)") {
                        tapCount += 1
                    }
                    .buttonStyle(.borderless)

                    Toggle("完了も表示", isOn: $showsDone)
                }

                Section("Todo") {
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
                }

                Section("追加") {
                    HStack {
                        TextField("新しいタスク", text: $newTitle)
                        Button("追加") {
                            store.add(title: newTitle)
                            newTitle = ""
                        }
                        .buttonStyle(.borderless)
                        .disabled(!canAdd)
                    }
                }
            }
            .navigationDestination(for: TodoItem.self) { item in
                TodoDetailView(item: item)
            }
            .navigationTitle("ToDo")
            .toolbar {
                EditButton()
            }
        }
    }
}

#Preview {
    ContentView()
        .environment(TodoStore.sample)
}
