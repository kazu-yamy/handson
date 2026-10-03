import Foundation
import Testing
@testable import HandsonApp

struct TodoDTOTests {

    // 実際の API のレスポンス（先頭 2 件。2 件目の completed だけ true に変えてある）
    static let json = """
    [
      { "userId": 1, "id": 1, "title": "delectus aut autem", "completed": false },
      { "userId": 1, "id": 2, "title": "quis ut nam facilis et officia qui", "completed": true }
    ]
    """

    @Test func decodesArray() throws {
        let todos = try JSONDecoder().decode([TodoDTO].self, from: Data(Self.json.utf8))
        #expect(todos.count == 2)
        #expect(todos[0].title == "delectus aut autem")
        #expect(todos[1].completed)
    }

    @Test func encodesToJSON() throws {
        let dto = TodoDTO(userId: 1, id: 0, title: "牛乳を買う", completed: false)
        let data = try JSONEncoder().encode(dto)
        let object = try #require(try JSONSerialization.jsonObject(with: data) as? [String: Any])
        #expect(object["title"] as? String == "牛乳を買う")
        #expect(object["completed"] as? Bool == false)
        #expect(object.count == 4)
    }

    @Test func convertsToTodoItem() {
        let done = TodoItem(dto: TodoDTO(userId: 1, id: 2, title: "A", completed: true))
        #expect(done.title == "A")
        #expect(done.isDone)
        #expect(done.priority == .medium)

        let open = TodoItem(dto: TodoDTO(userId: 1, id: 3, title: "B", completed: false))
        #expect(!open.isDone)
    }

    // キー名が違う場合 1: CodingKeys で対応づける
    struct Renamed: Codable {
        let title: String
        let isCompleted: Bool

        enum CodingKeys: String, CodingKey {
            case title
            case isCompleted = "completed"
        }
    }

    @Test func codingKeysMapsDifferentKeyNames() throws {
        let json = #"{ "title": "A", "completed": true }"#
        let value = try JSONDecoder().decode(Renamed.self, from: Data(json.utf8))
        #expect(value.isCompleted)
    }

    // キー名が違う場合 2: snake_case を camelCase に自動変換する
    struct SnakeCase: Decodable {
        let userId: Int
        let dueDate: String
    }

    @Test func convertFromSnakeCase() throws {
        let json = #"{ "user_id": 7, "due_date": "2026-10-03" }"#
        let decoder = JSONDecoder()
        decoder.keyDecodingStrategy = .convertFromSnakeCase
        let value = try decoder.decode(SnakeCase.self, from: Data(json.utf8))
        #expect(value.userId == 7)
        #expect(value.dueDate == "2026-10-03")
    }

    @Test func typeMismatchThrowsDecodingError() {
        let json = #"{ "userId": 1, "id": 1, "title": "A", "completed": "yes" }"#
        do {
            _ = try JSONDecoder().decode(TodoDTO.self, from: Data(json.utf8))
            Issue.record("デコードが成功してしまった")
        } catch DecodingError.typeMismatch(let type, let context) {
            print("DEBUG typeMismatch:", type, context.codingPath.map(\.stringValue), context.debugDescription)
            #expect("\(type)" == "Bool")
        } catch {
            Issue.record("想定外のエラー: \(error)")
        }
    }

    @Test func missingKeyThrowsDecodingError() {
        let json = #"{ "userId": 1, "id": 1, "title": "A" }"#
        do {
            _ = try JSONDecoder().decode(TodoDTO.self, from: Data(json.utf8))
            Issue.record("デコードが成功してしまった")
        } catch DecodingError.keyNotFound(let key, let context) {
            print("DEBUG keyNotFound:", key.stringValue, context.debugDescription)
            #expect(key.stringValue == "completed")
        } catch {
            Issue.record("想定外のエラー: \(error)")
        }
    }
}
