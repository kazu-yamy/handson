//
//  HandsonAppApp.swift
//  HandsonApp
//

import SwiftUI

@main
struct HandsonAppApp: App {
    @State private var store = TodoStore.sample

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environment(store)
        }
    }
}
