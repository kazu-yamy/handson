package com.handson.android01.data

import com.handson.android01.kotlin.Todo
import com.handson.android01.ui.screens.FakeDataStore
import com.handson.android01.ui.screens.FakeTodoDao
import com.handson.android01.ui.screens.FakeTodoServer
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

// 「取得済み」の印（DataStore）で、初回の取得を 1 回だけにする判断のテスト。偽の DAO・偽のサーバー・偽の DataStore だけで動く
class TodoRepositoryTest {

    private val todos = listOf(Todo(1, "write"), Todo(2, "test", done = true))

    private val settings = SettingsRepository(FakeDataStore())
    private val server = FakeTodoServer(todos)

    // 取得済みの印があれば、DB が空でも取得しない（全件削除して再起動した場合）
    @Test
    fun fetchedMark_skipsFetchEvenWhenDbIsEmpty() = runTest {
        settings.markInitialFetchDone()
        val repository = TodoRepository(FakeTodoDao(), server.api, settings)

        assertEquals(0, repository.fetchInitialTodosIfNeeded())

        assertEquals(0, server.requestCount)
    }

    // 取得に成功したら保存して、印を付ける。2 回目は取得しない
    @Test
    fun successfulFetch_savesTodosAndMarksAsFetched() = runTest {
        val repository = TodoRepository(FakeTodoDao(), server.api, settings)

        assertEquals(2, repository.fetchInitialTodosIfNeeded())
        assertEquals(0, repository.fetchInitialTodosIfNeeded())

        assertTrue(settings.isInitialFetchDone())
        assertEquals(1, server.requestCount)
    }

    // バックアップは、DB の全件を 1 回で送り、成功したら時刻を残す
    @Test
    fun backup_sendsAllTodosAndRecordsTime() = runTest {
        val repository = TodoRepository(FakeTodoDao(todos), server.api, settings)

        assertEquals(2, repository.backupTodos())

        assertEquals(1, server.requestCount)
        assertNotNull(settings.lastBackupAt.first())
    }

    // 失敗したら時刻を残さず、例外をそのまま投げる（やり直すかは Worker が決める）
    @Test
    fun failedBackup_doesNotRecordTime() = runTest {
        server.status = HttpStatusCode.InternalServerError
        val repository = TodoRepository(FakeTodoDao(todos), server.api, settings)

        assertTrue(runCatching { repository.backupTodos() }.isFailure)

        assertNull(settings.lastBackupAt.first())
    }

    // 取得に失敗したら、印を付けない（再試行でもう一度取得できる）
    @Test
    fun failedFetch_doesNotMarkAsFetched() = runTest {
        server.status = HttpStatusCode.InternalServerError
        val repository = TodoRepository(FakeTodoDao(), server.api, settings)

        try {
            repository.fetchInitialTodosIfNeeded()
            fail("取得の失敗が例外で投げられるはず")
        } catch (_: Exception) {
            // 失敗は例外のまま投げられる（表示は ViewModel の仕事）
        }

        assertFalse(settings.isInitialFetchDone())
    }

    // 印が無くても DB に Todo があれば（印を付ける前の版で使っていた端末）、取得せずに印だけ付ける。API の一覧で上書きしないため
    @Test
    fun existingTodosWithoutMark_areKeptAndMarked() = runTest {
        val repository = TodoRepository(FakeTodoDao(listOf(Todo(9, "mine", done = true))), server.api, settings)

        assertEquals(0, repository.fetchInitialTodosIfNeeded())

        assertEquals(0, server.requestCount)
        assertTrue(settings.isInitialFetchDone())
    }
}
