package com.handson.android01.data

import com.handson.android01.kotlin.Todo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Todo の入口。画面（ViewModel）は DAO と API を直接触らず、ここだけを使う。
// 唯一の情報源は Room。API は、DB が空のときに最初の一覧を取ってくるためだけに使う
class TodoRepository(
    private val dao: TodoDao,
    private val api: TodoApi,
) {
    // DB の一覧。変更のたびに最新の一覧が流れる
    val todos: Flow<List<Todo>> = dao.observeAll().map { list -> list.map { it.toTodo() } }

    // DB が空のときだけ、API から取得して保存する。保存した件数を返す（取得しなかったときは 0）。
    // 失敗した場合は、例外をそのまま投げる（失敗の表示は ViewModel の仕事）
    suspend fun refreshIfEmpty(): Int {
        if (dao.count() > 0) return 0
        val todos = api.fetchTodos().map { it.toEntity() }
        dao.insertAll(todos)
        return todos.size
    }

    suspend fun add(title: String) = dao.insert(TodoEntity(title = title, done = false))

    suspend fun setDone(id: Int, done: Boolean) = dao.setDone(id, done)

    suspend fun delete(id: Int) = dao.deleteById(id)

    // 削除の「元に戻す」。同じ id で入れ直すので、一覧の同じ位置（id の順）に戻る
    suspend fun restore(todo: Todo) = dao.insert(todo.toEntity())

    suspend fun deleteDone() = dao.deleteDone()
}
