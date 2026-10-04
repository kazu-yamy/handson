package com.handson.android01.data

import com.handson.android01.kotlin.Todo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// Todo の入口。画面（ViewModel）は DAO と API を直接触らず、ここだけを使う。
// 唯一の情報源は Room。API は、最初の 1 回だけ一覧を取ってくるために使う（取得済みかどうかは DataStore の設定に残す）
@Singleton
class TodoRepository @Inject constructor(
    private val dao: TodoDao,
    private val api: TodoApi,
    private val settings: SettingsRepository,
) {
    // DB の一覧。変更のたびに最新の一覧が流れる
    val todos: Flow<List<Todo>> = dao.observeAll().map { list -> list.map { it.toTodo() } }

    // 「取得済み」の印が無いときだけ、API から取得して保存する。保存した件数を返す（取得しなかったときは 0）。
    // 全件を削除して DB が空になっても、印があれば取り直さない。
    // 印が無くても DB に Todo があれば（印を付ける前の版で使っていた端末）、上書きを避けて、取得せずに印だけ付ける。
    // 失敗した場合は、印を付けずに例外をそのまま投げる（失敗の表示は ViewModel の仕事。再試行でもう一度取得できる）
    suspend fun fetchInitialTodosIfNeeded(): Int {
        if (settings.isInitialFetchDone()) return 0
        if (dao.count() > 0) {
            settings.markInitialFetchDone()
            return 0
        }
        val todos = api.fetchTodos().map { it.toEntity() }
        dao.insertAll(todos)
        settings.markInitialFetchDone()
        return todos.size
    }

    suspend fun add(title: String) = dao.insert(TodoEntity(title = title, done = false))

    suspend fun setDone(id: Int, done: Boolean) = dao.setDone(id, done)

    suspend fun delete(id: Int) = dao.deleteById(id)

    // 削除の「元に戻す」。同じ id で入れ直すので、一覧の同じ位置（id の順）に戻る
    suspend fun restore(todo: Todo) = dao.insert(todo.toEntity())

    suspend fun deleteDone() = dao.deleteDone()

    // DB の一覧を、まるごとサーバーへ送る。ローカルが正なので、送るだけで、サーバーからは何も取り込まない。
    // 成功したら送った時刻を残し、送った件数を返す。失敗したら例外をそのまま投げる（やり直すかどうかは Worker が決める）
    suspend fun backupTodos(): Int {
        val todos = dao.observeAll().first().map { it.toTodo() }
        api.uploadBackup(todos)
        settings.setLastBackupAt(System.currentTimeMillis())
        return todos.size
    }
}
