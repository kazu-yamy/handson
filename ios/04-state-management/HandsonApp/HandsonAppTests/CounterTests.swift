import Testing
@testable import HandsonApp

@MainActor
struct CounterTests {

    @Test func structIsCopiedOnAssignment() {
        var a = Counter()
        var b = a
        b.increment()
        a.increment()
        a.increment()
        #expect(a.count == 2)
        #expect(b.count == 1)
    }

    @Test func classIsSharedOnAssignment() {
        let a = CounterBox()
        let b = a
        b.increment()
        a.increment()
        #expect(a.count == 2)
        #expect(a === b)
    }
}
