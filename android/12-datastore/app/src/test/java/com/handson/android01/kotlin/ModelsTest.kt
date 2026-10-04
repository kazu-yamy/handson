package com.handson.android01.kotlin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelsTest {
    private val todo = Todo(id = 1, title = "Kotlin を学ぶ")

    @Test
    fun dataClass_hasDefaultValue() {
        assertEquals(false, todo.done)
    }

    @Test
    fun dataClass_copyChangesOnlySpecifiedField() {
        val done = todo.copy(done = true)
        assertEquals(true, done.done)
        assertEquals(todo.title, done.title)
        // copy は新しいインスタンスを返し、元の値は変わらない
        assertEquals(false, todo.done)
    }

    @Test
    fun dataClass_equalsComparesFields() {
        assertEquals(Todo(1, "Kotlin を学ぶ"), todo)
        assertNotEquals(Todo(2, "Kotlin を学ぶ"), todo)
    }

    @Test
    fun dataClass_destructuring() {
        val (id, title, done) = todo
        assertEquals(1, id)
        assertEquals("Kotlin を学ぶ", title)
        assertEquals(false, done)
    }

    @Test
    fun dataClass_toStringShowsFields() {
        assertTrue(todo.toString().startsWith("Todo(id=1, title=Kotlin を学ぶ, done=false)"))
    }

    @Test
    fun sealedWhen_coversAllStates() {
        assertEquals("読み込み中", stateLabel(UiState.Loading))
        assertEquals("2 件", stateLabel(UiState.Success(listOf(todo, todo.copy(id = 2)))))
        assertEquals("エラー: 通信失敗", stateLabel(UiState.Error("通信失敗")))
    }

    @Test
    fun enum_whenReturnsLabel() {
        assertEquals("低", priorityLabel(Priority.LOW))
        assertEquals("中", priorityLabel(Priority.MEDIUM))
        assertEquals("高", priorityLabel(Priority.HIGH))
        assertEquals(listOf("LOW", "MEDIUM", "HIGH"), Priority.entries.map { it.name })
    }
}
