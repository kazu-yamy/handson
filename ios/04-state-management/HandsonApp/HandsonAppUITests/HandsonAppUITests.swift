//
//  HandsonAppUITests.swift
//  HandsonAppUITests
//

import XCTest

final class HandsonAppUITests: XCTestCase {

    override func setUpWithError() throws {
        continueAfterFailure = false
    }

    @MainActor
    func testExample() throws {
        let app = XCUIApplication()
        app.launch()
    }
}
