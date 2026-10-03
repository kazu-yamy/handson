import Foundation
import Testing
@testable import HandsonApp

@MainActor
@Suite("TodoItem")
struct TodoItemTests {

    @Test func priorityRawValueAndCases() {
        #expect(Priority.high.rawValue == 3)
        #expect(Priority(rawValue: 1) == .low)
        #expect(Priority(rawValue: 9) == nil)
        #expect(Priority.allCases.map(\.label) == ["低", "中", "高"])
    }

    @Test func statusTextForTodo() {
        let item = TodoItem(title: "買い物")
        #expect(item.statusText == "未完了")
    }

    @Test func statusTextForDone() {
        let date = Date(timeIntervalSince1970: 0)
        let item = TodoItem(title: "買い物", priority: .high, status: .done(date))
        #expect(item.statusText.hasPrefix("完了 ("))
    }

    // 入れ子のスイート。表示名は「ステータス表示」
    @Suite("ステータス表示")
    struct StatusText {
        @Test("未完了は「未完了」と表示する")
        func todo() {
            #expect(TodoItem(title: "A").statusText == "未完了")
        }

        @Test("完了は「完了 (日付)」と表示する")
        func done() {
            let item = TodoItem(title: "A", status: .done(Date(timeIntervalSince1970: 0)))
            #expect(item.statusText.hasPrefix("完了 ("))
        }
    }

    @Test("toggle で未完了と完了が入れ替わる")
    func toggleSwitchesBetweenTodoAndDone() {
        var item = TodoItem(title: "買い物")
        #expect(!item.isDone)
        item.toggle()
        #expect(item.isDone)
        item.toggle()
        #expect(!item.isDone)
        #expect(item.statusText == "未完了")
    }
}
