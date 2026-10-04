package com.handson.android01.data

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import io.ktor.client.engine.android.Android
import kotlinx.coroutines.Dispatchers

// アプリ全体で使う部品の置き場所（手書きの最小の DI）。ViewModel は、ここから TodoApi や TodoRepository を受け取る。
// HttpClient は接続を使い回すので、画面ごとに作らず、アプリで 1 つを使い回す。DB も同じで、アプリで 1 つだけ作る。
// Hilt を導入する項目では、この object をなくして、Hilt に作らせる
object AppContainer {

    private lateinit var appContext: Context

    // TodoApplication.onCreate から 1 度だけ呼ぶ
    fun init(context: Context) {
        appContext = context.applicationContext
    }

    private val database: AppDatabase by lazy {
        Room.databaseBuilder<AppDatabase>(appContext, "todos.db")
            // クエリを実行するスレッド。ここでは IO 用のスレッドプールを指定する
            .setQueryCoroutineContext(Dispatchers.IO)
            // SQLite を動かすドライバー。省略しても既定でこのドライバーが使われるが、何を使っているか分かるように明示する
            .setDriver(AndroidSQLiteDriver())
            .build()
    }

    private val defaultTodoApi: TodoApi by lazy { TodoApi(createTodoHttpClient(Android.create())) }

    private val defaultTodoRepository: TodoRepository by lazy { TodoRepository(database.todoDao(), defaultTodoApi) }

    // テストが、偽の DAO を使う TodoRepository に差し替えるための入口
    var todoRepositoryOverride: TodoRepository? = null

    val todoRepository: TodoRepository get() = todoRepositoryOverride ?: defaultTodoRepository
}
