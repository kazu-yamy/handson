import Foundation
import Testing
@testable import HandsonApp

@MainActor
struct TodoStoreTests {

    @Test func addAppendsItemWithDefaults() {
        let store = TodoStore()
        store.add(title: "買い物")
        #expect(store.items.count == 1)
        #expect(store.items[0].title == "買い物")
        #expect(store.items[0].priority == .medium)
        #expect(!store.items[0].isDone)
    }

    @Test func toggleChangesStatus() {
        let store = TodoStore(items: [TodoItem(title: "買い物")])
        store.toggle(store.items[0])
        #expect(store.items[0].isDone)
        store.toggle(store.items[0])
        #expect(!store.items[0].isDone)
    }

    @Test func toggleIgnoresUnknownItem() {
        let store = TodoStore(items: [TodoItem(title: "買い物")])
        store.toggle(TodoItem(title: "存在しない"))
        #expect(!store.items[0].isDone)
    }
}
