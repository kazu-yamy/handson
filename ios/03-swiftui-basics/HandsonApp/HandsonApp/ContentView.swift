//
//  ContentView.swift
//  HandsonApp
//

import SwiftUI

struct ContentView: View {
    let profile = Profile(name: "Taro Yamada", nickname: "Taro")

    let items: [TodoItem] = [
        TodoItem(title: "牛乳を買う", priority: .high),
        TodoItem(title: "メールを返す", priority: .low, status: .done(Date())),
        TodoItem(title: "SwiftUI を勉強する"),
    ]

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            ProfileHeader(profile: profile)

            Text("Todo")
                .font(.headline)

            // List は 05 で扱う。ここでは ForEach を VStack に並べる
            VStack(alignment: .leading, spacing: 12) {
                ForEach(items, id: \.title) { item in
                    TodoRow(item: item)
                }
            }

            if items.isEmpty {
                Text("Todo はありません")
                    .foregroundStyle(.secondary)
            }

            Spacer()
        }
        .padding()
    }
}

#Preview {
    ContentView()
}
