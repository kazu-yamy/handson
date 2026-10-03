import Testing
@testable import HandsonApp

@Suite("#expect と #require")
struct ExpectRequireTests {

    @Test("#expect は失敗しても続行する（式の中身が展開される）")
    func expectShowsExpandedValues() {
        let item = TodoItem(title: "買い物")
        #expect(item.title == "買い物")
        #expect(item.priority == .medium)
        #expect(!item.isDone)
    }

    @Test("#require は失敗するとテストを中断し、オプショナルをアンラップする")
    func requireUnwrapsOptional() throws {
        let titles = ["A", "B"]
        let first = try #require(titles.first)  // nil なら、ここでテストが終わる
        #expect(first == "A")
    }
}
