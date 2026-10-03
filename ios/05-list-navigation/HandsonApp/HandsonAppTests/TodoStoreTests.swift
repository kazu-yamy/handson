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

    @Test func removeDeletesItemsAtOffsets() {
        let store = TodoStore(items: [
            TodoItem(title: "A"), TodoItem(title: "B"), TodoItem(title: "C"),
        ])
        store.remove(at: IndexSet(integer: 1))
        #expect(store.items.map(\.title) == ["A", "C"])
    }

    @Test func moveReordersItems() {
        let store = TodoStore(items: [
            TodoItem(title: "A"), TodoItem(title: "B"), TodoItem(title: "C"),
        ])
        // 先頭の A を末尾（offset 3）へ
        store.move(from: IndexSet(integer: 0), to: 3)
        #expect(store.items.map(\.title) == ["B", "C", "A"])
    }
}
