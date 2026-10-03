import Foundation
import SwiftData
import Testing
@testable import HandsonApp

@MainActor
struct TodoLocationTests {
    @Test func 作ったばかりのTodoには場所がない() {
        let record = TodoRecord(title: "x")
        #expect(record.latitude == nil)
        #expect(record.longitude == nil)
        #expect(!record.hasLocation)
    }

    @Test func setLocationで場所が入りclearLocationで消える() {
        let record = TodoRecord(title: "x")

        record.setLocation(latitude: 35.681236, longitude: 139.767125)
        #expect(record.hasLocation)
        #expect(record.latitude == 35.681236)

        record.clearLocation()
        #expect(!record.hasLocation)
        #expect(record.longitude == nil)
    }

    @Test func 緯度だけでは場所ありとみなさない() {
        let record = TodoRecord(title: "x")
        record.latitude = 35.0
        #expect(!record.hasLocation)
    }

    @Test func 保存して取得し直しても場所が残る() throws {
        let configuration = ModelConfiguration(isStoredInMemoryOnly: true)
        let container = try ModelContainer(for: TodoRecord.self, configurations: configuration)
        let context = container.mainContext
        let record = TodoRecord(title: "東京駅")
        record.setLocation(latitude: 35.681236, longitude: 139.767125)
        context.insert(record)
        try context.save()

        let fetched = try #require(try context.fetch(FetchDescriptor<TodoRecord>()).first)
        #expect(fetched.latitude == 35.681236)
        #expect(fetched.longitude == 139.767125)
    }
}
