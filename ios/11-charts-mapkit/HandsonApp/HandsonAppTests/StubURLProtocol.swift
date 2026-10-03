import Foundation
import Synchronization
@testable import HandsonApp

// 送信されたリクエストの記録。POST のテストで「何を送ったか」を確かめるために使う
struct RecordedRequest: Sendable {
    let method: String
    let url: URL
    let contentType: String?
    let body: Data
}

// ネットワークに出ずに、決めた応答を返す URLProtocol。
// テストごとに別のホスト名（stub-<UUID>.test）を使い、ホスト名をキーにして
// 応答と記録を保持するので、テストを並列実行しても互いに干渉しない。
final class StubURLProtocol: URLProtocol {
    typealias Handler = @Sendable (URLRequest) throws -> (statusCode: Int, body: Data)

    // 可変な static は Mutex で守る（`static var` のままだと Swift 6 のコンパイルエラー）
    private static let handlers = Mutex<[String: Handler]>([:])
    private static let recorded = Mutex<[String: [RecordedRequest]]>([:])

    // このスタブ専用のセッションとクライアントを作る
    static func makeClient(statusCode: Int = 200, json: String) -> TodoAPIClient {
        makeClient { _ in (statusCode, Data(json.utf8)) }
    }

    static func makeClient(handler: @escaping Handler) -> TodoAPIClient {
        let host = "stub-\(UUID().uuidString.lowercased()).test"
        handlers.withLock { $0[host] = handler }
        let configuration = URLSessionConfiguration.ephemeral
        configuration.protocolClasses = [StubURLProtocol.self]
        return TodoAPIClient(
            session: URLSession(configuration: configuration),
            baseURL: URL(string: "https://\(host)")!
        )
    }

    static func requests(for client: TodoAPIClient) -> [RecordedRequest] {
        recorded.withLock { $0[client.baseURL.host()!] ?? [] }
    }

    override class func canInit(with request: URLRequest) -> Bool { true }
    override class func canonicalRequest(for request: URLRequest) -> URLRequest { request }

    override func startLoading() {
        guard let url = request.url, let host = url.host(),
              let handler = Self.handlers.withLock({ $0[host] }) else {
            client?.urlProtocol(self, didFailWithError: URLError(.badURL))
            return
        }
        let record = RecordedRequest(
            method: request.httpMethod ?? "GET",
            url: url,
            contentType: request.value(forHTTPHeaderField: "Content-Type"),
            body: Self.bodyData(of: request)
        )
        Self.recorded.withLock { $0[host, default: []].append(record) }
        do {
            let (statusCode, body) = try handler(request)
            let response = HTTPURLResponse(url: url, statusCode: statusCode, httpVersion: "HTTP/1.1", headerFields: ["Content-Type": "application/json"])!
            client?.urlProtocol(self, didReceive: response, cacheStoragePolicy: .notAllowed)
            client?.urlProtocol(self, didLoad: body)
            client?.urlProtocolDidFinishLoading(self)
        } catch {
            client?.urlProtocol(self, didFailWithError: error)
        }
    }

    override func stopLoading() {}

    // URLSession は httpBody を httpBodyStream に変換して URLProtocol に渡すので、ストリームから読む
    private static func bodyData(of request: URLRequest) -> Data {
        if let body = request.httpBody { return body }
        guard let stream = request.httpBodyStream else { return Data() }
        stream.open()
        defer { stream.close() }
        var data = Data()
        var buffer = [UInt8](repeating: 0, count: 1024)
        while stream.hasBytesAvailable {
            let count = stream.read(&buffer, maxLength: buffer.count)
            if count <= 0 { break }
            data.append(buffer, count: count)
        }
        return data
    }
}
