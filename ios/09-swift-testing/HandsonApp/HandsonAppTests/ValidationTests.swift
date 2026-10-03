import Testing
@testable import HandsonApp

@MainActor
@Suite("名前の検証", .tags(.validation))
struct ValidationTests {

    // 1 つのコレクション: 要素ごとに 1 ケースのテストになる
    @Test("正しい名前は例外を投げない", arguments: ["Taro", "A", "1234567890"])
    func validNameDoesNotThrow(name: String) throws {
        try validate(name: name)
    }

    @Test("空の名前は .empty を投げる")
    func emptyNameThrows() {
        #expect(throws: ValidationError.empty) {
            try validate(name: "")
        }
    }

    @Test("長すぎる名前は .tooLong(max: 10) を投げる", arguments: ["12345678901", "あいうえおかきくけこさ"])
    func tooLongNameThrowsWithAssociatedValue(name: String) {
        #expect(throws: ValidationError.tooLong(max: 10)) {
            try validate(name: name)
        }
    }

    // zip: 入力と期待値を 1 対 1 で対応づける（3 ケース）
    @Test("Result 版は入力に対応した結果を返す", arguments: zip(
        ["Taro", "", "12345678901"],
        [Result<String, ValidationError>.success("Taro"), .failure(.empty), .failure(.tooLong(max: 10))]
    ))
    func resultVersion(name: String, expected: Result<String, ValidationError>) {
        #expect(validatedName(name) == expected)
    }

    // 2 つのコレクションをそのまま渡すと全組み合わせ（3 × 2 = 6 ケース）
    @Test("全組み合わせ: どんな名前でも 11 文字以上は失敗、10 文字以下は成功", arguments: [1, 10, 11], ["a", "あ"])
    func allCombinations(length: Int, character: String) {
        let name = String(repeating: character, count: length)
        #expect(validatedName(name).isSuccess == (length <= 10))
    }
}

extension Result {
    fileprivate var isSuccess: Bool {
        if case .success = self { true } else { false }
    }
}
