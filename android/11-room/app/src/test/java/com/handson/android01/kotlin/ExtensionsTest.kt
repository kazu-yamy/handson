package com.handson.android01.kotlin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ExtensionsTest {
    private val todos = sampleTodos()

    @Test
    fun stringExtension_toTitleCase() {
        assertEquals("Hello Kotlin World", "hello kotlin world".toTitleCase())
        assertEquals("", "".toTitleCase())
    }

    @Test
    fun listExtension_doneCount() {
        assertEquals(2, todos.doneCount())
        assertEquals(0, emptyList<Todo>().doneCount())
    }

    @Test
    fun listExtension_summaryLine() {
        assertEquals("4 件中 2 件完了", todos.summaryLine())
    }

    @Test
    fun apply_configuresAndReturnsReceiver() {
        val draft = draftWithApply("買い物")
        assertEquals("買い物", draft.title)
        assertFalse(draft.done)
    }

    @Test
    fun also_addsAndReturnsSameList() {
        val list = mutableListOf("a")
        val returned = addAndRecord(list, "b")
        assertEquals(listOf("a", "b"), returned)
        assertEquals(true, returned === list)
    }

    @Test
    fun run_andWith_computeSameResult() {
        assertEquals("4 件中 2 件完了", summarizeWithRun(todos))
        assertEquals("4 件中 2 件完了", summarizeWithWith(todos))
    }
}
