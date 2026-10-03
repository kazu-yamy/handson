//
//  HandsonAppApp.swift
//  HandsonApp
//

import SwiftUI

@main
struct HandsonAppApp: App {
    @State private var store = TodoStore(repository: TodoRepository(client: TodoAPIClient()))

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environment(store)
        }
    }
}
