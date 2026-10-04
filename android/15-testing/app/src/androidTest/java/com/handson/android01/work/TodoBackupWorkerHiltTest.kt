package com.handson.android01.work

import android.content.Context
import androidx.hilt.work.HiltWorkerFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import com.handson.android01.data.SettingsRepository
import com.handson.android01.data.TodoApi
import com.handson.android01.data.TodoDao
import com.handson.android01.data.toEntity
import com.handson.android01.di.FakeApiModule
import com.handson.android01.ui.screens.FakeTodoServer
import com.handson.android01.ui.screens.demoTodos
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

// Worker を、アプリと同じ HiltWorkerFactory で作ってテストする。
// 14 の TodoBackupWorkerTest は自前の工場で Worker を作ったので、Hilt の組み立て（@HiltWorker・@AssistedInject と、
// Worker が受け取る部品）は確かめていなかった。こちらは、その組み立てごと確かめる
@HiltAndroidTest
// このクラスだけ、androidTest 共通の偽の API を外す（下の @BindValue の偽物と、同じ型の部品が 2 つになるのを防ぐ）
@UninstallModules(FakeApiModule::class)
@RunWith(AndroidJUnit4::class)
class TodoBackupWorkerHiltTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    private val server = FakeTodoServer(demoTodos(3))

    // @BindValue: このフィールドの値を、このテストの間だけ Hilt の部品（TodoApi）として使う。
    // TodoRepository → TodoBackupWorker と、Hilt が組み立てる部品に、この偽物が渡る
    @BindValue
    @JvmField
    val api: TodoApi = server.api

    // hiltRule.inject() を呼ぶと、Hilt がここに部品を入れる。dao・settings は @Singleton なので、Worker（TodoRepository）が使うのと同じインスタンス
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var dao: TodoDao

    @Inject
    lateinit var settings: SettingsRepository

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    private fun worker(): TodoBackupWorker =
        TestListenableWorkerBuilder<TodoBackupWorker>(context)
            .setWorkerFactory(workerFactory)
            .build()

    @Test
    fun hiltWorker_backsUpTodosFromInjectedDao() {
        // 注入された DAO（FakeDataModule の偽物）に入れた Todo を、Worker が送る
        runBlocking { dao.insertAll(demoTodos(2).map { it.toEntity() }) }

        val result = runBlocking { worker().doWork() }

        assertEquals(ListenableWorker.Result.success(), result)
        assertEquals(1, server.requestCount)
        // Worker が書いた時刻を、注入された SettingsRepository から読める（同じインスタンス）
        assertNotNull(runBlocking { settings.lastBackupAt.first() })
    }

    @Test
    fun hiltWorker_retriesOnServerError() {
        server.status = HttpStatusCode.InternalServerError

        val result = runBlocking { worker().doWork() }

        assertEquals(ListenableWorker.Result.retry(), result)
        assertNull(runBlocking { settings.lastBackupAt.first() })
    }
}
