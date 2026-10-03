import Foundation
import Observation

@Observable
final class TodoStore {
    var items: [TodoItem]

    init(items: [TodoItem] = []) {
        self.items = items
    }

    func add(title: String, priority: Priority = .medium) {
        items.append(TodoItem(title: title, priority: priority))
    }

    func toggle(_ item: TodoItem) {
        guard let index = items.firstIndex(where: { $0.title == item.title }) else { return }
        items[index].toggle()
    }

    static let sample = TodoStore(items: [
        TodoItem(title: "牛乳を買う", priority: .high),
        TodoItem(title: "メールを返す", priority: .low, status: .done(Date())),
        TodoItem(title: "SwiftUI を勉強する"),
    ])
}
