import Foundation

// 完了 / 未完了の件数を数える。画面にもグラフにも依存しない純粋な型
struct TodoStats: Equatable {
    let done: Int
    let pending: Int

    var total: Int { done + pending }

    // グラフに渡す 1 件分のデータ。Chart の ForEach は Identifiable な配列を取る
    struct Entry: Identifiable {
        let status: String
        let count: Int
        var id: String { status }
    }

    var entries: [Entry] {
        [Entry(status: "完了", count: done), Entry(status: "未完了", count: pending)]
    }

    init(records: [TodoRecord]) {
        done = records.filter(\.isDone).count
        pending = records.count - done
    }
}
