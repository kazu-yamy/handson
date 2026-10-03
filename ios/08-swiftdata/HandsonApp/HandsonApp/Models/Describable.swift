protocol Describable {
    var summary: String { get }
}

// デフォルト実装
extension Describable {
    var summary: String { "(no summary)" }
}

extension Profile: Describable {
    var summary: String { "Profile: \(displayName)" }
}

extension TodoItem: Describable {
    var summary: String { "\(title) [\(priority.label)] \(statusText)" }
}

// 準拠するだけでデフォルト実装が使える
extension Counter: Describable {}

// ジェネリクス: 同じ型の配列
func summaries<T: Describable>(_ items: [T]) -> [String] {
    items.map(\.summary)
}

// 存在型: 異なる型を混ぜた配列
func summaries(of items: [any Describable]) -> [String] {
    items.map(\.summary)
}
