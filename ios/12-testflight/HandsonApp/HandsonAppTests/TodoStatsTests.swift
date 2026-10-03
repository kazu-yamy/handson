import Testing
@testable import HandsonApp

@MainActor
struct TodoStatsTests {
    @Test func 完了と未完了の件数を数える() {
        let finished = TodoRecord(title: "完了")
        finished.toggle()
        let records = [finished, TodoRecord(title: "未完了A"), TodoRecord(title: "未完了B")]

        let stats = TodoStats(records: records)

        #expect(stats.done == 1)
        #expect(stats.pending == 2)
        #expect(stats.total == 3)
    }

    @Test func 空の配列ならすべて0() {
        let stats = TodoStats(records: [])
        #expect(stats == TodoStats(records: []))
        #expect(stats.total == 0)
    }
}
