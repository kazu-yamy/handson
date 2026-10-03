//
//  HandsonAppApp.swift
//  HandsonApp
//

import SwiftData
import SwiftUI

@main
struct HandsonAppApp: App {
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
        .modelContainer(for: TodoRecord.self)
    }
}
