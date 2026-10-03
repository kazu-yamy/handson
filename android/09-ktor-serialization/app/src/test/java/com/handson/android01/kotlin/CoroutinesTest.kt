package com.handson.android01.kotlin

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CoroutinesTest {
    @Test
    fun delay_doesNotConsumeRealTime() = runTest {
        val startedAt = System.currentTimeMillis()
        val todos = fetchTodos()
        val elapsedRealMs = System.currentTimeMillis() - startedAt

        assertEquals(4, todos.size)
        // 仮想時間では 1000ms 進んでいる
        assertEquals(1000L, currentTime)
        // 実時間はほとんど消費していない
        assertTrue("実時間 ${elapsedRealMs}ms", elapsedRealMs < 1000)
    }

    @Test
    fun advanceTimeBy_controlsVirtualClock() = runTest {
        var finished = false
        launch {
            delay(500)
            finished = true
        }
        advanceTimeBy(499)
        assertFalse(finished)
        advanceTimeBy(1)
        runCurrent()
        assertTrue(finished)
        assertEquals(500L, currentTime)
    }

    @Test
    fun withContext_ioDispatcher_returnsResult() = runTest {
        assertEquals(4, fetchTodosOnIo().size)
    }

    @Test
    fun async_runsInParallel() = runTest {
        val (todos, count) = fetchTodosAndCount()
        assertEquals(4, todos.size)
        assertEquals(4, count)
        // 直列なら 2000ms かかるが、並列なので 1000ms
        assertEquals(1000L, currentTime)
    }
}
