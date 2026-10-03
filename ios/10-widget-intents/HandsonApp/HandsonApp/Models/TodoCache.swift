import Foundation

actor TodoCache {
    private var storage: [String: [TodoItem]] = [:]

    func value(for key: String) -> [TodoItem]? {
        storage[key]
    }

    func store(_ items: [TodoItem], for key: String) {
        storage[key] = items
    }
}
