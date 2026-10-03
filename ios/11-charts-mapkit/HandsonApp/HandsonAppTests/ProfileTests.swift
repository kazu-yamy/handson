import Testing
@testable import HandsonApp

@MainActor
@Suite("Profile")
struct ProfileTests {

    @Test("ニックネームがあれば displayName に使う")
    func displayNameUsesNicknameWhenPresent() {
        let profile = Profile(name: "Taro", nickname: "Tarou")
        #expect(profile.displayName == "Tarou")
    }

    @Test("ニックネームが無ければ name にフォールバックする")
    func displayNameFallsBackToName() {
        let profile = Profile(name: "Taro", nickname: nil)
        #expect(profile.displayName == "Taro")
    }

    @Test func greetingWithAndWithoutNickname() {
        #expect(Profile(name: "Taro", nickname: "T").greeting() == "Hello, T (Taro)!")
        #expect(Profile(name: "Taro", nickname: nil).greeting() == "Hello, Taro!")
    }

    @Test func labelAndOptionalChaining() {
        let none = Profile(name: "Taro", nickname: nil)
        let some = Profile(name: "Taro", nickname: "tt")
        #expect(none.label == "(no nickname)")
        #expect(none.nicknameUppercased == nil)
        #expect(some.nicknameUppercased == "TT")
    }
}
