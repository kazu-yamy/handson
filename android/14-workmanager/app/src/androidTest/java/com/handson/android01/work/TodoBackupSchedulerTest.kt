package com.handson.android01.work

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.Configuration
import androidx.work.NetworkType
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import com.handson.android01.data.SettingsRepository
import com.handson.android01.data.TodoRepository
import com.handson.android01.ui.screens.FakeDataStore
import com.handson.android01.ui.screens.FakeTodoDao
import com.handson.android01.ui.screens.FakeTodoServer
import com.handson.android01.ui.screens.demoTodos
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

// 予約（Periodic・条件）を、テスト用の WorkManager で確かめる。TestDriver で「条件がそろった」を起こす
@RunWith(AndroidJUnit4::class)
class TodoBackupSchedulerTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun periodicBackup_waitsForConstraintsThenRuns() {
        val settings = SettingsRepository(FakeDataStore())
        val server = FakeTodoServer(demoTodos(3))
        val repository = TodoRepository(FakeTodoDao(demoTodos(3)), server.api, settings)
        val config = Configuration.Builder()
            .setExecutor(SynchronousExecutor())
            .setWorkerFactory(object : WorkerFactory() {
                override fun createWorker(appContext: Context, workerClassName: String, workerParameters: WorkerParameters) =
                    TodoBackupWorker(appContext, workerParameters, repository)
            })
            .build()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)

        TodoBackupScheduler(context).schedulePeriodicBackup()
        TodoBackupScheduler(context).schedulePeriodicBackup() // 2 回呼んでも、一意な名前で 1 つ

        val workManager = WorkManager.getInstance(context)
        val info = workManager.getWorkInfosForUniqueWork(TodoBackupScheduler.UNIQUE_NAME).get().single()
        assertEquals(WorkInfo.State.ENQUEUED, info.state)
        assertEquals(NetworkType.CONNECTED, info.constraints.requiredNetworkType)
        assertTrue(info.constraints.requiresBatteryNotLow())
        assertEquals(TimeUnit.DAYS.toMillis(1), info.periodicityInfo?.repeatIntervalMillis)
        assertEquals(0, server.requestCount) // 条件がそろうまで動かない

        val driver = WorkManagerTestInitHelper.getTestDriver(context)!!
        driver.setAllConstraintsMet(info.id)

        // doWork はテストのスレッドで始まるが、Ktor（MockEngine）の通信の後は別のスレッドで再開するので、記録が付くまで待つ
        runBlocking { withTimeout(5_000) { settings.lastBackupAt.first { it != null } } }
        assertEquals(1, server.requestCount)
    }
}
