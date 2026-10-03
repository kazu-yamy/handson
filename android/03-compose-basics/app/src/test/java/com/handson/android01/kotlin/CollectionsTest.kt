package com.handson.android01.kotlin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CollectionsTest {
    private val todos = sampleTodos()

    @Test
    fun listOf_isReadOnlyView() {
        // 型は List。追加は関数としても用意されていない（コンパイルで防がれる）
        assertEquals(4, todos.size)
        assertEquals(1, todos.first().id)
    }

    @Test
    @Suppress("UNCHECKED_CAST")
    fun listOf_isReadOnlyOnlyByType_notAtRuntime() {
        // List は型の上で読み取り専用なだけ。キャストすれば実行時に変更でき、
        // 変更を試みると UnsupportedOperationException になる（listOf が返すリストは要素の追加・削除ができない（サイズ固定））
        val sneaky = todos as MutableList<Todo>
        val e = org.junit.Assert.assertThrows(UnsupportedOperationException::class.java) {
            sneaky.add(Todo(5, "強制追加"))
        }
        println("add message: ${e.message}")
    }

    @Test
    fun mutableList_canAddElements() {
        val titles = collectTitles(todos)
        titles.add("追加")
        assertEquals(5, titles.size)
        assertEquals("追加", titles.last())
    }

    @Test
    fun filter_and_map_withIt() {
        assertEquals(listOf("買い物", "Kotlin を学ぶ"), titlesOfDone(todos))
    }

    @Test
    fun sortedBy_withExplicitLambdaParameter() {
        assertEquals(listOf(4, 3, 2, 1), sortedByIdDescending(todos).map { it.id })
    }

    @Test
    fun groupBy_splitsByKey() {
        val groups = groupByDone(todos)
        assertEquals(listOf(1, 3), groups[true]!!.map { it.id })
        assertEquals(listOf(2, 4), groups[false]!!.map { it.id })
    }

    @Test
    fun firstOrNull_returnsNullWhenNotFound() {
        assertEquals("掃除", findTodo(todos, 2)?.title)
        assertNull(findTodo(todos, 99))
    }

    @Test
    fun trailingLambda_passesPredicate() {
        assertEquals(2, countWhere(todos) { !it.done })
    }
}
