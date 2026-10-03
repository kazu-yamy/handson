import Foundation
import Testing
@testable import HandsonApp

@MainActor
@Suite("既知の問題と confirmation")
struct KnownIssueAndConfirmationTests {

    // TodoStore.toggle は title で項目を探すので、同じタイトルが 2 つあると先頭の項目が切り替わる。
    // 直すまでの間、この失敗を「既知の問題」として記録する
    @Test("同じタイトルの 2 件目を toggle できる")
    func toggleSecondItemWithSameTitle() {
        let store = TodoStore(items: [TodoItem(title: "A"), TodoItem(title: "A")])
        withKnownIssue("toggle は title で探すため、同名の項目は先頭が切り替わる") {
            store.toggle(store.items[1])
            #expect(store.items[1].isDone)
        }
    }

    @Test("完了済みの項目を 1 件削除すると、コールバックが 1 回呼ばれる")
    func removeCompletedCallsBackOnce() async {
        let store = TodoStore(items: [
            TodoItem(title: "A"), TodoItem(title: "B", status: .done(.now)),
        ])
        await confirmation("onRemove が呼ばれる", expectedCount: 1) { removed in
            store.removeCompleted { _ in removed() }
        }
        #expect(store.items.map(\.title) == ["A"])
    }

    @Test("完了済みが無ければ、コールバックは呼ばれない")
    func removeCompletedNeverCallsBack() async {
        let store = TodoStore(items: [TodoItem(title: "A")])
        await confirmation("onRemove は呼ばれない", expectedCount: 0) { removed in
            store.removeCompleted { _ in removed() }
        }
        #expect(store.items.count == 1)
    }
}
