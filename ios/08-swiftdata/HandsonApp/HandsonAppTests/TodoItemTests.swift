import Foundation
import Testing
@testable import HandsonApp

@MainActor
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

    @Test func toggleSwitchesBetweenTodoAndDone() {
        var item = TodoItem(title: "買い物")
        #expect(!item.isDone)
        item.toggle()
        #expect(item.isDone)
        item.toggle()
        #expect(!item.isDone)
        #expect(item.statusText == "未完了")
    }
}
