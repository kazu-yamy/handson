import Foundation

nonisolated enum APIError: Error, Equatable {
    case invalidResponse
    case badStatus(Int)
}

// POST で送る本文。id はサーバーが決めるので持たない
private nonisolated struct NewTodo: Encodable {
    let title: String
    let completed: Bool
    let userId: Int
}

nonisolated struct TodoAPIClient {
    var session: URLSession = .shared
    var baseURL = URL(string: "https://jsonplaceholder.typicode.com")!

    @concurrent func fetchTodos(limit: Int) async throws -> [TodoDTO] {
        // https://jsonplaceholder.typicode.com/todos?_limit=5
        var components = URLComponents(url: baseURL.appending(path: "todos"), resolvingAgainstBaseURL: false)!
        components.queryItems = [URLQueryItem(name: "_limit", value: String(limit))]

        let (data, response) = try await session.data(from: components.url!)
        try checkStatus(response, expected: 200..<300)
        return try JSONDecoder().decode([TodoDTO].self, from: data)
    }

    @concurrent func createTodo(title: String) async throws -> TodoDTO {
        var request = URLRequest(url: baseURL.appending(path: "todos"))
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = try JSONEncoder().encode(NewTodo(title: title, completed: false, userId: 1))

        let (data, response) = try await session.data(for: request)
        try checkStatus(response, expected: 201..<202)   // 作成成功は 201 Created
        return try JSONDecoder().decode(TodoDTO.self, from: data)
    }

    private func checkStatus(_ response: URLResponse, expected: Range<Int>) throws {
        guard let http = response as? HTTPURLResponse else {
            throw APIError.invalidResponse
        }
        guard expected.contains(http.statusCode) else {
            throw APIError.badStatus(http.statusCode)
        }
    }
}
