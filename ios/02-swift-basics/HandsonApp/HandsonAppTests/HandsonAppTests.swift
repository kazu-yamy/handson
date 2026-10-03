//
//  HandsonAppTests.swift
//  HandsonAppTests
//

import Testing
@testable import HandsonApp

struct HandsonAppTests {

    @Test @MainActor func greetingContainsName() {
        #expect(greeting(for: "Swift") == "Hello, Swift!")
    }

}
