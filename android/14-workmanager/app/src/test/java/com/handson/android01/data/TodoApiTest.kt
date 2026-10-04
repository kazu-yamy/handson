package com.handson.android01.data

import com.handson.android01.kotlin.Todo
import io.ktor.client.call.NoTransformationFoundException
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.JsonConvertException
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// TodoApi の単体テスト（JVM 上。ネットワークには出ない）。MockEngine が、決めたレスポンスを返す。
// createTodoHttpClient を通すので、本番と同じ JSON の設定・ステータスの検査・タイムアウトが効く
class TodoApiTest {

    private val json = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

    private val body = """
        [
          {"userId": 1, "id": 1, "title": "delectus aut autem", "completed": false},
          {"userId": 1, "id": 2, "title": "quis ut nam", "completed": true}
        ]
    """.trimIndent()

    private fun api(
        requestTimeoutMillis: Long = 15_000L,
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
    ) = TodoApi(createTodoHttpClient(MockEngine(handler), requestTimeoutMillis))

    @Test
    fun fetchTodos_decodesJsonIntoTodos() = runTest {
        val api = api { respond(body, HttpStatusCode.OK, json) }

        val todos = api.fetchTodos()

        assertEquals(listOf(Todo(1, "delectus aut autem", done = false), Todo(2, "quis ut nam", done = true)), todos)
    }

    // 送ったリクエストは、MockEngine の中で確かめられる
    @Test
    fun fetchTodos_sendsGetToTodosWithLimit() = runTest {
        var request: HttpRequestData? = null
        val api = api {
            request = it
            respond("[]", HttpStatusCode.OK, json)
        }

        api.fetchTodos(limit = 5)

        assertEquals(HttpMethod.Get, request?.method)
        assertEquals("https://jsonplaceholder.typicode.com/todos?_limit=5", request?.url.toString())
    }

    // バックアップは POST /todos に、Todo の一覧を JSON の配列で送る
    @Test
    fun uploadBackup_postsTodosAsJsonArray() = runTest {
        var request: HttpRequestData? = null
        val api = api {
            request = it
            respond("{}", HttpStatusCode.Created, json)
        }

        api.uploadBackup(listOf(Todo(1, "write", done = true), Todo(2, "test")))

        assertEquals(HttpMethod.Post, request?.method)
        assertEquals("/todos", request?.url?.encodedPath)
        val sent = request!!.body.toByteArray().decodeToString()
        assertTrue(sent, sent.contains(""""title":"write"""") && sent.contains(""""completed":true"""))
    }

    @Test
    fun fetchTodos_ignoresUnknownKeys() = runTest {
        val api = api {
            respond("""[{"userId": 1, "id": 1, "title": "a", "completed": false, "priority": "high"}]""", HttpStatusCode.OK, json)
        }

        assertEquals(listOf(Todo(1, "a")), api.fetchTodos())
    }

    // expectSuccess = true なので、2xx 以外は本文のデコードに進まず、ステータスの例外になる
    @Test
    fun fetchTodos_whenNotFound_throwsClientRequestException() = runTest {
        val api = api { respond("""{"error": "not found"}""", HttpStatusCode.NotFound, json) }

        val e = failureOf<ClientRequestException> { api.fetchTodos() }

        assertEquals(404, e.response.status.value)
    }

    @Test
    fun fetchTodos_whenServerError_throwsServerResponseException() = runTest {
        val api = api { respond("oops", HttpStatusCode.InternalServerError) }

        val e = failureOf<ServerResponseException> { api.fetchTodos() }

        assertEquals(500, e.response.status.value)
    }

    @Test
    fun fetchTodos_whenJsonIsBroken_throwsJsonConvertException() = runTest {
        val api = api { respond("""[{"userId": 1, "id": 1, "title":""", HttpStatusCode.OK, json) }

        failureOf<JsonConvertException> { api.fetchTodos() }
    }

    // 必須の項目（title）が無い JSON も、デコードの失敗になる
    @Test
    fun fetchTodos_whenRequiredFieldIsMissing_throwsJsonConvertException() = runTest {
        val api = api { respond("""[{"userId": 1, "id": 1, "completed": false}]""", HttpStatusCode.OK, json) }

        failureOf<JsonConvertException> { api.fetchTodos() }
    }

    // Content-Type が JSON でないレスポンスは、ContentNegotiation が変換できない
    @Test
    fun fetchTodos_whenContentTypeIsMissing_throwsNoTransformationFoundException() = runTest {
        val api = api { respond(body, HttpStatusCode.OK) }

        failureOf<NoTransformationFoundException> { api.fetchTodos() }
    }

    // 応答が来ないとき: requestTimeoutMillis を過ぎると HttpRequestTimeoutException。応答の遅れは実時間（MockEngine は別のスレッドで動く）
    @Test
    fun fetchTodos_whenNoResponse_throwsTimeoutException() = runTest {
        val api = api(requestTimeoutMillis = 100L) {
            delay(5_000)
            respond(body, HttpStatusCode.OK, json)
        }

        failureOf<HttpRequestTimeoutException> { api.fetchTodos() }
    }

    // block が E の例外で失敗することを確かめて、その例外を返す
    private inline fun <reified E : Throwable> failureOf(block: () -> Unit): E {
        val e = runCatching(block).exceptionOrNull()
        assertTrue("期待した例外: ${E::class.simpleName}、実際: $e", e is E)
        return e as E
    }
}
