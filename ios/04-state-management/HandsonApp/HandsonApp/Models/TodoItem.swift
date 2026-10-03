import Foundation

enum Priority: Int, CaseIterable {
    case low = 1
    case medium
    case high

    var label: String {
        switch self {
        case .low: "低"
        case .medium: "中"
        case .high: "高"
        }
    }
}

// 関連値付きの enum
enum Status {
    case todo
    case done(Date)
}

struct TodoItem {
    let title: String
    var priority: Priority = .medium
    var status: Status = .todo

    var isDone: Bool {
        if case .done = status {
            return true
        }
        return false
    }

    mutating func toggle() {
        switch status {
        case .todo:
            status = .done(Date())
        case .done:
            status = .todo
        }
    }

    var statusText: String {
        switch status {
        case .todo:
            "未完了"
        case .done(let date):
            "完了 (\(date.formatted(.iso8601.year().month().day())))"
        }
    }
}
