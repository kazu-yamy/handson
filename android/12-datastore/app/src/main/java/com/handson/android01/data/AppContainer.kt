package com.handson.android01.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import io.ktor.client.engine.android.Android
import kotlinx.coroutines.Dispatchers

// DataStore は、ファイル（datastore/settings.preferences_pb）ごとに、アプリで 1 つだけ作る。
// preferencesDataStore のプロパティ委譲は、トップレベルに 1 回だけ書く（クラスの中に書くと、インスタンスのたびに別の DataStore が作られる）
private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

// アプリ全体で使う部品の置き場所（手書きの最小の DI）。ViewModel は、ここから TodoRepository と SettingsRepository を受け取る。
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

    private val defaultSettingsRepository: SettingsRepository by lazy { SettingsRepository(appContext.settingsDataStore) }

    private val defaultTodoRepository: TodoRepository by lazy { TodoRepository(database.todoDao(), defaultTodoApi, defaultSettingsRepository) }

    // テストが、偽の DAO・偽の DataStore を使うものに差し替えるための入口
    var todoRepositoryOverride: TodoRepository? = null
    var settingsRepositoryOverride: SettingsRepository? = null

    val settingsRepository: SettingsRepository get() = settingsRepositoryOverride ?: defaultSettingsRepository

    val todoRepository: TodoRepository get() = todoRepositoryOverride ?: defaultTodoRepository
}
