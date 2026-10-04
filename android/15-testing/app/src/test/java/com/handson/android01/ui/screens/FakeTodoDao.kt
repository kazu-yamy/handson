package com.handson.android01.ui.screens

import com.handson.android01.data.TodoDao
import com.handson.android01.data.TodoEntity
import com.handson.android01.data.toEntity
import com.handson.android01.kotlin.Todo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

// テスト用の偽の DAO。本物の Room は使わず、メモリ上のリストを MutableStateFlow に入れて動かす。
// どの関数も中断せずに値を書き換えるので、テストの中で書き込みの直後に結果を確かめられる。
// initial は「DB にすでに入っている」状態を作るためのもの
class FakeTodoDao(initial: List<Todo> = emptyList()) : TodoDao {

    private val rows = MutableStateFlow(initial.map { it.toEntity() })

    // これまでに使った最大の id。行を消しても戻さない（本物の AUTOINCREMENT と同じく、削除した id を使い回さない）
    private var lastId = initial.maxOfOrNull { it.id } ?: 0

    override fun observeAll(): Flow<List<TodoEntity>> = rows.map { list -> list.sortedBy { it.id } }

    override suspend fun count(): Int = rows.value.size

    // id が 0 なら、lastId + 1 を振る。同じ id があれば置き換える（REPLACE）
    override suspend fun insert(todo: TodoEntity) {
        val saved = if (todo.id == 0) todo.copy(id = lastId + 1) else todo
        lastId = maxOf(lastId, saved.id)
        rows.update { list -> list.filterNot { it.id == saved.id } + saved }
    }

    override suspend fun insertAll(todos: List<TodoEntity>) = todos.forEach { insert(it) }

    override suspend fun setDone(id: Int, done: Boolean) {
        rows.update { list -> list.map { if (it.id == id) it.copy(done = done) else it } }
    }

    override suspend fun deleteById(id: Int) {
        rows.update { list -> list.filterNot { it.id == id } }
    }

    override suspend fun deleteDone() {
        rows.update { list -> list.filterNot { it.done } }
    }
}
