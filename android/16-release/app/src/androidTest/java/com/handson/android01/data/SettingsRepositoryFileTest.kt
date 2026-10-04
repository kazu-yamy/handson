package com.handson.android01.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.handson.android01.di.settingsCorruptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.job
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

// 本物の DataStore（ファイル）で SettingsRepository を動かす。偽物（FakeDataStore）では確かめられない、
// 「ファイルに書かれて、作り直しても読める」ことと「壊れたファイル」を確かめる
@RunWith(AndroidJUnit4::class)
class SettingsRepositoryFileTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    // テストごとに別のファイル（アプリの設定 settings.preferences_pb には触れない）。
    // Preferences DataStore のファイルは、拡張子が preferences_pb でなければならない
    private val file = File(context.filesDir, "datastore/test-${UUID.randomUUID()}.preferences_pb")
    private val scopes = mutableListOf<CoroutineScope>()

    // アプリの preferencesDataStore と同じ設定（壊れたときの扱い）で作る。
    // scope は DataStore が動く場所で、止めるとファイルを手放す
    private fun createDataStore(): DataStore<Preferences> {
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob()).also { scopes += it }
        return PreferenceDataStoreFactory.create(
            corruptionHandler = settingsCorruptionHandler,
            scope = scope,
            produceFile = { file },
        )
    }

    @After
    fun tearDown() {
        scopes.forEach { it.cancel() }
        file.delete()
    }

    @Test
    fun savedSettings_surviveNewDataStore(): Unit = runBlocking {
        val first = SettingsRepository(createDataStore())
        first.setFilter(TodoFilter.Done)
        first.markInitialFetchDone()
        first.setLastBackupAt(1_000L)
        assertTrue(file.exists())

        // 1 つ目を止める（アプリの終了に当たる）。止めずに 2 つ目を作ると、同じファイルに 2 つの DataStore があることになって例外が出る
        scopes.last().coroutineContext.job.cancelAndJoin()

        val second = SettingsRepository(createDataStore())
        assertEquals(TodoFilter.Done, second.filter.first())
        assertTrue(second.isInitialFetchDone())
        assertEquals(1_000L, second.lastBackupAt.first())
    }

    @Test
    fun corruptedFile_isReplacedWithEmptySettings(): Unit = runBlocking {
        // protobuf として途中で切れたデータ
        file.parentFile?.mkdirs()
        file.writeBytes(byteArrayOf(0x0A, 0x7F))

        val settings = SettingsRepository(createDataStore())
        assertEquals(TodoFilter.All, settings.filter.first())
        assertFalse(settings.isInitialFetchDone())
        assertNull(settings.lastBackupAt.first())
    }
}
