import Testing
@testable import HandsonApp

@MainActor
struct ValidationTests {

    @Test func validNameDoesNotThrow() throws {
        try validate(name: "Taro")
    }

    @Test func emptyNameThrows() {
        #expect(throws: ValidationError.self) {
            try validate(name: "")
        }
    }

    @Test func tooLongNameThrowsWithAssociatedValue() {
        #expect(throws: ValidationError.tooLong(max: 10)) {
            try validate(name: "12345678901")
        }
    }

    @Test func resultVersion() {
        #expect(validatedName("Taro") == .success("Taro"))
        #expect(validatedName("") == .failure(.empty))
    }
}
