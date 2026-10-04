package com.handson.android01.work

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.handson.android01.data.SettingsRepository
import com.handson.android01.data.TodoRepository
import com.handson.android01.ui.screens.FakeDataStore
import com.handson.android01.ui.screens.FakeTodoDao
import com.handson.android01.ui.screens.FakeTodoServer
import com.handson.android01.ui.screens.demoTodos
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

// Worker を、WorkManager に登録せずに直接作って doWork を呼ぶ。
// HiltWorkerFactory の代わりに、偽物をつないだ TodoRepository を渡す工場を使う
@RunWith(AndroidJUnit4::class)
class TodoBackupWorkerTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val settings = SettingsRepository(FakeDataStore())
    private val server = FakeTodoServer(demoTodos(3))

    private fun worker(runAttemptCount: Int = 0): TodoBackupWorker =
        TestListenableWorkerBuilder<TodoBackupWorker>(context)
            .setRunAttemptCount(runAttemptCount)
            .setWorkerFactory(object : WorkerFactory() {
                override fun createWorker(appContext: Context, workerClassName: String, workerParameters: WorkerParameters) =
                    TodoBackupWorker(appContext, workerParameters, TodoRepository(FakeTodoDao(demoTodos(3)), server.api, settings))
            })
            .build()

    @Test
    fun success_recordsBackupTime() {
        val result = runBlocking { worker().doWork() }

        assertEquals(ListenableWorker.Result.success(), result)
        assertEquals(1, server.requestCount)
        assertNotNull(runBlocking { settings.lastBackupAt.first() })
    }

    // 5xx は、時間をおけば直るかもしれないので retry
    @Test
    fun serverError_retries() {
        server.status = HttpStatusCode.InternalServerError

        val result = runBlocking { worker().doWork() }

        assertEquals(ListenableWorker.Result.retry(), result)
        assertNull(runBlocking { settings.lastBackupAt.first() })
    }

    // 4xx は、何度送っても同じなので failure
    @Test
    fun clientError_fails() {
        server.status = HttpStatusCode.BadRequest

        val result = runBlocking { worker().doWork() }

        assertEquals(ListenableWorker.Result.failure(), result)
    }

    // retry は MAX_RETRIES 回まで。それを超えたらあきらめる
    @Test
    fun serverError_failsAfterMaxRetries() {
        server.status = HttpStatusCode.InternalServerError

        val result = runBlocking { worker(runAttemptCount = TodoBackupWorker.MAX_RETRIES).doWork() }

        assertEquals(ListenableWorker.Result.failure(), result)
    }
}
