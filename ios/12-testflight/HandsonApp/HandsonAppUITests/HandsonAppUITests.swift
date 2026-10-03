//
//  HandsonAppUITests.swift
//  HandsonAppUITests
//

import XCTest

// UI テストは XCTest で書く（Apple は UI テストとパフォーマンステストには XCTest を使うよう案内している）
final class HandsonAppUITests: XCTestCase {

    @MainActor
    private func launchApp() -> XCUIApplication {
        let app = XCUIApplication()
        // アプリ側がこの引数を見て、メモリ上だけのデータベースに切り替える
        app.launchArguments = ["-uiTesting"]
        app.launch()
        return app
    }

    override func setUpWithError() throws {
        continueAfterFailure = false
    }

    @MainActor
    func testAddedTaskAppearsInList() throws {
        let app = launchApp()
        XCTAssertTrue(app.staticTexts["ToDo はまだありません"].waitForExistence(timeout: 5))

        let field = app.textFields["新しいタスク"]
        XCTAssertTrue(field.waitForExistence(timeout: 5))
        field.tap()
        field.typeText("牛乳を買う")
        app.buttons["追加"].tap()

        XCTAssertTrue(app.staticTexts["牛乳を買う"].waitForExistence(timeout: 5))
        XCTAssertFalse(app.staticTexts["ToDo はまだありません"].exists)
    }

    @MainActor
    func testToggleMarksTaskAsDone() throws {
        let app = launchApp()

        let field = app.textFields["新しいタスク"]
        XCTAssertTrue(field.waitForExistence(timeout: 5))
        field.tap()
        field.typeText("メールを返す")
        app.buttons["追加"].tap()

        let complete = app.buttons["完了にする"]
        XCTAssertTrue(complete.waitForExistence(timeout: 5))
        complete.tap()

        XCTAssertTrue(app.buttons["未完了に戻す"].waitForExistence(timeout: 5))
    }

    @MainActor
    func testTapMapSetsAndClearsLocation() throws {
        let app = launchApp()

        let field = app.textFields["新しいタスク"]
        XCTAssertTrue(field.waitForExistence(timeout: 5))
        field.tap()
        field.typeText("牛乳を買う")
        app.buttons["追加"].tap()
        app.staticTexts["牛乳を買う"].tap()

        // 詳細画面: 最初は場所が無い
        XCTAssertTrue(app.staticTexts["地図をタップして場所を設定"].waitForExistence(timeout: 5))

        // 地図をタップすると、緯度経度の表示に変わる
        app.maps.firstMatch.coordinate(withNormalizedOffset: CGVector(dx: 0.3, dy: 0.4)).tap()
        let coordinateLabel = app.staticTexts.matching(NSPredicate(format: "label BEGINSWITH '緯度'")).firstMatch
        XCTAssertTrue(coordinateLabel.waitForExistence(timeout: 5))

        // 場所を消すと元に戻る
        app.buttons["場所を消す"].tap()
        XCTAssertTrue(app.staticTexts["地図をタップして場所を設定"].waitForExistence(timeout: 5))
    }
}
