package com.handson.android01.data

import com.handson.android01.kotlin.Todo
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

// 練習用の公開 API（jsonplaceholder）から Todo を取得する。
// HttpClient は引数で受け取る（テストでは、通信しない MockEngine 入りの HttpClient を渡すため）
class TodoApi(
    private val client: HttpClient,
    private val baseUrl: String = "https://jsonplaceholder.typicode.com",
) {
    // 取得して、アプリの Todo に変換して返す。失敗時は例外を投げる（呼び出し側で捕まえる）。
    // suspend 関数だが、通信は Ktor のエンジン（Android）がバックグラウンドのスレッド（Dispatchers.IO）で行うので、メインスレッドから呼んでも画面は固まらない
    suspend fun fetchTodos(limit: Int = 20): List<Todo> {
        val dtos: List<TodoDto> = client.get("$baseUrl/todos") { parameter("_limit", limit) }.body()
        return dtos.map { it.toTodo() }
    }

    // 一覧をまとめて送る（バックアップ）。失敗時は例外を投げる（2xx 以外も expectSuccess で例外になる）。
    // 練習用の API は 201 Created を返すが、保存はしない。送信と応答までは本物の API と同じ
    suspend fun uploadBackup(todos: List<Todo>) {
        client.post("$baseUrl/todos") {
            // 本文を JSON にする。ContentNegotiation が、List<TodoDto> を JSON の配列に変換する
            contentType(ContentType.Application.Json)
            setBody(todos.map { it.toDto() })
        }
    }
}

// アプリでも、テストでも同じ設定の HttpClient を作る。違うのはエンジンだけ（アプリ = Android、テスト = MockEngine）
fun createTodoHttpClient(
    engine: HttpClientEngine,
    // 1 回の呼び出し（送信から受信の完了まで）の上限
    requestTimeoutMillis: Long = 15_000L,
): HttpClient = HttpClient(engine) {
    // 2xx 以外のステータスを例外にする。既定は false で、404 や 500 でもそのまま本文のデコードに進んでしまう
    expectSuccess = true
    // 何も指定しないと、Android エンジンは接続 100 秒・読み取り 100 秒（パケットの間隔）だけで、呼び出し全体の上限は無い。
    // 少しずつ送り続ける相手だと、読み込み中のまま終わらないので、全体の上限を足す。
    // 接続のタイムアウトは 15 秒に揃えられる（Android エンジンは、呼び出し全体の上限より長い接続のタイムアウトを、その値まで縮める）。読み取りの 100 秒は、そのまま
    install(HttpTimeout) {
        this.requestTimeoutMillis = requestTimeoutMillis
    }
    install(ContentNegotiation) {
        json(
            Json {
                // API の JSON にあって TodoDto に無い項目（今回は無いが、API が項目を足したとき）を無視する
                ignoreUnknownKeys = true
            },
        )
    }
}
