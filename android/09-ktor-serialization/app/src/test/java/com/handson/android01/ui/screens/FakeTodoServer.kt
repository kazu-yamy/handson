package com.handson.android01.ui.screens

import com.handson.android01.data.TodoApi
import com.handson.android01.data.TodoDto
import com.handson.android01.data.createTodoHttpClient
import com.handson.android01.kotlin.Todo
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockEngineConfig
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// テスト用の偽のサーバー。MockEngine が、ネットワークに出ずにレスポンスを返す。
// status と todos は、テストの途中で書き換えられる（失敗 → 再試行で成功、を作るため）。
// dispatcher を渡すと、エンジンの処理がそのディスパッチャーで動く。テスト用のディスパッチャーを渡せば、delayMillis が仮想時間になる
class FakeTodoServer(
    var todos: List<Todo>,
    var status: HttpStatusCode = HttpStatusCode.OK,
    private val delayMillis: Long = 0L,
    dispatcher: CoroutineDispatcher? = null,
) {
    private val engine = MockEngine(
        MockEngineConfig().apply {
            this.dispatcher = dispatcher
            addHandler {
                delay(delayMillis)
                val body = Json.encodeToString(todos.map { TodoDto(userId = 1, id = it.id, title = it.title, completed = it.done) })
                // Content-Type を付けないと、ContentNegotiation が JSON として読んでくれない
                respond(body, status, headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()))
            }
        },
    )

    val api = TodoApi(createTodoHttpClient(engine))

    val requestCount: Int get() = engine.requestHistory.size
}
