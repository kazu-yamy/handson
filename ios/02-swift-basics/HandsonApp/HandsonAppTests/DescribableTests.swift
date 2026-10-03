import Testing
@testable import HandsonApp

@MainActor
struct DescribableTests {

    @Test func conformingTypesProvideSummary() {
        #expect(Profile(name: "Taro", nickname: nil).summary == "Profile: Taro")
        #expect(TodoItem(title: "買い物").summary == "買い物 [中] 未完了")
    }

    @Test func defaultImplementationIsUsed() {
        #expect(Counter().summary == "(no summary)")
    }

    @Test func genericFunctionTakesHomogeneousArray() {
        let items = [TodoItem(title: "A"), TodoItem(title: "B", priority: .high)]
        #expect(summaries(items) == ["A [中] 未完了", "B [高] 未完了"])
    }

    @Test func existentialArrayMixesTypes() {
        let items: [any Describable] = [Profile(name: "Taro", nickname: nil), Counter()]
        #expect(summaries(of: items) == ["Profile: Taro", "(no summary)"])
    }
}
