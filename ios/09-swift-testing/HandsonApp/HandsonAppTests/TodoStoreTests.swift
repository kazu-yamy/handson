import Foundation
import Testing
@testable import HandsonApp

@MainActor
@Suite("TodoStore")
struct TodoStoreTests {
    // @Test ごとに TodoStoreTests のインスタンスが新しく作られるので、
    // init が共通のセットアップになる。あるテストが store を変更しても、他のテストには見えない
    let store: TodoStore

    init() {
        store = TodoStore(items: [
            TodoItem(title: "A"), TodoItem(title: "B"), TodoItem(title: "C"),
        ])
    }

    @Test("add は既定値（優先度 中・未完了）で末尾に追加する")
    func addAppendsItemWithDefaults() async {
        await store.add(title: "買い物")
        #expect(store.items.count == 4)
        #expect(store.items[3].title == "買い物")
        #expect(store.items[3].priority == .medium)
        #expect(!store.items[3].isDone)
    }

    @Test("toggle で完了と未完了が入れ替わる")
    func toggleChangesStatus() {
        store.toggle(store.items[0])
        #expect(store.items[0].isDone)
        store.toggle(store.items[0])
        #expect(!store.items[0].isDone)
    }

    @Test("存在しない項目の toggle は何もしない")
    func toggleIgnoresUnknownItem() {
        store.toggle(TodoItem(title: "存在しない"))
        #expect(store.items.allSatisfy { !$0.isDone })
    }

    @Test("remove は指定位置の項目を削除する")
    func removeDeletesItemsAtOffsets() {
        store.remove(at: IndexSet(integer: 1))
        #expect(store.items.map(\.title) == ["A", "C"])
    }

    @Test("move で項目を並べ替える")
    func moveReordersItems() {
        // 先頭の A を末尾（offset 3）へ
        store.move(from: IndexSet(integer: 0), to: 3)
        #expect(store.items.map(\.title) == ["B", "C", "A"])
    }
}
