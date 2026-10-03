import Foundation

// async の中では Thread.isMainThread を直接呼べないので、同期関数に切り出す
nonisolated func runningOnMainThread() -> Bool {
    Thread.isMainThread
}

nonisolated struct TodoSummary {
    let doneCount: Int
    let totalCount: Int
    let ranOnMainThread: Bool

    var text: String {
        "\(doneCount) / \(totalCount) 件完了"
    }
}

struct TodoSummarizer {
    // @concurrent: メインアクターの外（グローバルな並行プール）で実行する
    @concurrent
    func summarize(_ items: [TodoItem]) async -> TodoSummary {
        try? await Task.sleep(for: .milliseconds(300))  // 重い計算の代わり
        let done = items.filter { $0.isDone }.count
        return TodoSummary(
            doneCount: done,
            totalCount: items.count,
            ranOnMainThread: runningOnMainThread()
        )
    }
}
