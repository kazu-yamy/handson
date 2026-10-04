package com.handson.android01.data

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.runner.RunWith

// 本物の Room。ファイルを作らないメモリ上の DB（テストが終われば消える）なので、アプリの DB（todos.db）には触れない。
// アプリと同じドライバーを使う
@RunWith(AndroidJUnit4::class)
class RoomTodoDaoTest : TodoDaoContract() {

    private val database = Room.inMemoryDatabaseBuilder<AppDatabase>(ApplicationProvider.getApplicationContext<Context>())
        .setDriver(AndroidSQLiteDriver())
        .build()

    override val dao: TodoDao = database.todoDao()

    @After
    fun closeDatabase() = database.close()
}
