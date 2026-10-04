package com.handson.android01.data

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

// TodoDao が守るべき約束（契約）。同じテストを、本物の Room（RoomTodoDaoTest）と偽物（FakeTodoDaoTest）の両方で動かす。
// 偽物が本物とずれていると、偽物を使うテスト（UI テストのほとんど）は「本物では起きないこと」を確かめてしまう。
// 抽象クラスなので、これ自体はテストとして実行されない（名前も …Test にしない）
abstract class TodoDaoContract {

    protected abstract val dao: TodoDao

    private fun entity(id: Int, title: String, done: Boolean = false) = TodoEntity(id = id, title = title, done = done)

    private suspend fun ids(): List<Int> = dao.observeAll().first().map { it.id }

    // 一覧は id の順（入れた順ではない）
    @Test
    fun observeAll_isSortedById(): Unit = runBlocking {
        dao.insertAll(listOf(entity(3, "c"), entity(1, "a"), entity(2, "b")))
        assertEquals(listOf(1, 2, 3), ids())
    }

    // 同じ id で入れると置き換わる（REPLACE）。行は増えない
    @Test
    fun insert_withSameId_replacesRow(): Unit = runBlocking {
        dao.insert(entity(1, "before"))
        dao.insert(entity(1, "after", done = true))
        assertEquals(listOf(entity(1, "after", done = true)), dao.observeAll().first())
        assertEquals(1, dao.count())
    }

    // id = 0 で入れると新しい id が振られる。削除した id は使い回さない（autoGenerate = SQLite の AUTOINCREMENT）
    @Test
    fun insert_withIdZero_doesNotReuseDeletedId(): Unit = runBlocking {
        dao.insertAll(listOf(entity(1, "a"), entity(2, "b")))
        dao.deleteById(2)
        dao.insert(entity(0, "new"))
        assertEquals(listOf(1, 3), ids())
    }

    // 完了にした行だけが、まとめて消える
    @Test
    fun deleteDone_removesOnlyDoneRows(): Unit = runBlocking {
        dao.insertAll(listOf(entity(1, "a"), entity(2, "b"), entity(3, "c")))
        dao.setDone(1, true)
        dao.setDone(3, true)
        dao.deleteDone()
        assertEquals(listOf(2), ids())
    }
}
