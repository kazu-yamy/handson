package com.handson.android01.data

import io.ktor.client.engine.android.Android

// アプリ全体で使う部品の置き場所（手書きの最小の DI）。ViewModel は、ここから TodoApi を受け取る。
// HttpClient は接続を使い回すので、画面ごとに作らず、アプリで 1 つを使い回す。
// Hilt を導入する項目では、この object をなくして、Hilt に作らせる
object AppContainer {

    private val defaultTodoApi: TodoApi by lazy { TodoApi(createTodoHttpClient(Android.create())) }

    // テストが、通信しない TodoApi に差し替えるための入口（本番のコードは触らない）
    var todoApiOverride: TodoApi? = null

    val todoApi: TodoApi get() = todoApiOverride ?: defaultTodoApi
}
