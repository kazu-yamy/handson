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
        VStack(alignment: .leading, spacing: 16) {
            ProfileHeader(profile: profile)

            Button("タップ回数: \(tapCount)") {
                tapCount += 1
            }

            Toggle("完了も表示", isOn: $showsDone)

            Text("Todo")
                .font(.headline)

            VStack(alignment: .leading, spacing: 12) {
                ForEach($store.items, id: \.title) { $item in
                    if showsDone || !item.isDone {
                        TodoRow(item: $item)
                    }
                }
            }

            HStack {
                TextField("新しいタスク", text: $newTitle)
                    .textFieldStyle(.roundedBorder)
                Button("追加") {
                    store.add(title: newTitle)
                    newTitle = ""
                }
                .disabled(!canAdd)
            }

            Spacer()
        }
        .padding()
    }
}

#Preview {
    ContentView()
        .environment(TodoStore.sample)
}
