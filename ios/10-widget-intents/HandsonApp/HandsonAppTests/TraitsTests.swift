import Foundation
import Testing
@testable import HandsonApp

@MainActor
@Suite("トレイト")
struct TraitsTests {

    @Test("タグを付ける", .tags(.validation, .slow))
    func tagged() {
        #expect(validatedName("Taro") == .success("Taro"))
    }

    @Test("条件を満たすときだけ実行する", .enabled(if: ProcessInfo.processInfo.environment["RUN_EXTRA"] != nil))
    func enabledIf() {
        #expect(Bool(true))
    }

    @Test("理由を付けて無効化する", .disabled("API の仕様が決まるまで止める"))
    func disabled() {
        #expect(Bool(false))
    }

    @Test("関連する課題を記録する", .bug("https://example.com/issues/123", "名前の上限を変えたい"))
    func bug() {
        #expect(validatedName("Taro") == .success("Taro"))
    }

    @Test("制限時間を付ける", .timeLimit(.minutes(1)))
    func timeLimit() async throws {
        try await Task.sleep(for: .milliseconds(10))
    }
}

// 既定では、テストは並列に実行される。.serialized を付けたスイートの中は 1 つずつ順番に実行する
@Suite("直列実行", .serialized)
struct SerializedTests {
    @Test(arguments: [1, 2, 3])
    func runsOneAtATime(n: Int) async throws {
        try await Task.sleep(for: .milliseconds(100))
        #expect(n > 0)
    }
}
