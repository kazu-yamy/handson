import Foundation
import Testing
@testable import HandsonApp

struct TodoAPIClientTests {

    @Test func fetchTodosDecodesResponse() async throws {
        let client = StubURLProtocol.makeClient(json: """
        [
          { "userId": 1, "id": 1, "title": "delectus aut autem", "completed": false },
          { "userId": 1, "id": 2, "title": "quis ut nam facilis et officia qui", "completed": true }
        ]
        """)
        let todos = try await client.fetchTodos(limit: 2)
        #expect(todos.map(\.title) == ["delectus aut autem", "quis ut nam facilis et officia qui"])
        #expect(todos[1].completed)
    }

    @Test func fetchTodosSendsGetWithLimitQuery() async throws {
        let client = StubURLProtocol.makeClient(json: "[]")
        _ = try await client.fetchTodos(limit: 5)
        let request = try #require(StubURLProtocol.requests(for: client).first)
        #expect(request.method == "GET")
        #expect(request.url.path() == "/todos")
        #expect(request.url.query() == "_limit=5")
    }

    @Test func notFoundThrowsBadStatus() async {
        let client = StubURLProtocol.makeClient(statusCode: 404, json: "{}")
        await #expect(throws: APIError.badStatus(404)) {
            try await client.fetchTodos(limit: 5)
        }
    }

    @Test func serverErrorThrowsBadStatus() async {
        let client = StubURLProtocol.makeClient(statusCode: 500, json: "")
        await #expect(throws: APIError.badStatus(500)) {
            try await client.fetchTodos(limit: 5)
        }
    }

    @Test func brokenJSONThrowsDecodingError() async {
        let client = StubURLProtocol.makeClient(json: #"[{ "userId": 1, "id": 1, "title": "A", "completed": "yes" }]"#)
        do {
            _ = try await client.fetchTodos(limit: 1)
            Issue.record("成功してしまった")
        } catch is DecodingError {
            // 期待どおり
        } catch {
            Issue.record("想定外のエラー: \(error)")
        }
    }

    @Test func networkFailureThrowsURLError() async {
        let client = StubURLProtocol.makeClient { _ in throw URLError(.notConnectedToInternet) }
        do {
            _ = try await client.fetchTodos(limit: 1)
            Issue.record("成功してしまった")
        } catch let error as URLError {
            #expect(error.code == .notConnectedToInternet)
        } catch {
            Issue.record("想定外のエラー: \(error)")
        }
    }
}

@MainActor
struct TodoRepositoryAPITests {

    @Test func fetchAllConvertsDTOsToItems() async throws {
        let client = StubURLProtocol.makeClient(json: """
        [
          { "userId": 1, "id": 1, "title": "delectus aut autem", "completed": false },
          { "userId": 1, "id": 2, "title": "fugiat veniam minus", "completed": true }
        ]
        """)
        let repository = TodoRepository(client: client)
        let items = try await repository.fetchAll(useCache: false)
        #expect(items.map(\.title) == ["delectus aut autem", "fugiat veniam minus"])
        #expect(!items[0].isDone)
        #expect(items[1].isDone)
    }

    @Test func storeShowsErrorMessageOnBadStatus() async {
        let client = StubURLProtocol.makeClient(statusCode: 404, json: "{}")
        let store = TodoStore(repository: TodoRepository(client: client))
        await store.load(useCache: false)
        #expect(store.items.isEmpty)
        #expect(store.errorMessage == "読み込みに失敗しました")
        #expect(!store.isLoading)
    }
}

struct TodoAPIClientCreateTests {

    @Test func createTodoSendsPostWithJSONBody() async throws {
        let client = StubURLProtocol.makeClient(statusCode: 201, json: """
        { "title": "牛乳を買う", "completed": false, "userId": 1, "id": 201 }
        """)
        let created = try await client.createTodo(title: "牛乳を買う")
        #expect(created.id == 201)
        #expect(created.title == "牛乳を買う")

        let request = try #require(StubURLProtocol.requests(for: client).first)
        #expect(request.method == "POST")
        #expect(request.url.path() == "/todos")
        #expect(request.contentType == "application/json")
        let body = try #require(try JSONSerialization.jsonObject(with: request.body) as? [String: Any])
        #expect(body["title"] as? String == "牛乳を買う")
        #expect(body["completed"] as? Bool == false)
        #expect(body["userId"] as? Int == 1)
        #expect(body["id"] == nil)
    }

    @Test func createTodoThrowsWhenStatusIsNot201() async {
        // 200 は成功ステータスだが、作成の成功条件（201）には合わない
        let client = StubURLProtocol.makeClient(statusCode: 200, json: "{}")
        await #expect(throws: APIError.badStatus(200)) {
            try await client.createTodo(title: "A")
        }
    }
}

@MainActor
struct TodoStoreAddTests {

    @Test func addAppendsServerResponse() async {
        let client = StubURLProtocol.makeClient(statusCode: 201, json: """
        { "title": "牛乳を買う", "completed": false, "userId": 1, "id": 201 }
        """)
        let store = TodoStore(repository: TodoRepository(client: client))
        await store.add(title: "牛乳を買う", priority: .high)
        #expect(store.items.map(\.title) == ["牛乳を買う"])
        #expect(store.items[0].priority == .high)
        #expect(store.errorMessage == nil)
    }

    @Test func addShowsErrorOnFailure() async {
        let client = StubURLProtocol.makeClient(statusCode: 500, json: "")
        let store = TodoStore(repository: TodoRepository(client: client))
        await store.add(title: "A")
        #expect(store.items.isEmpty)
        #expect(store.errorMessage == "追加に失敗しました")
    }
}
