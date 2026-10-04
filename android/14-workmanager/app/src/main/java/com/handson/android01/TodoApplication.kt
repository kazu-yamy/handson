package com.handson.android01

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.handson.android01.work.TodoBackupScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

// Hilt の入口。この注釈を付けると、アプリ全体の部品の置き場所（コンポーネント）がここで作られる。
// Configuration.Provider: WorkManager は、最初に使われたとき（WorkManager.getInstance）に、ここから設定を読んで初期化する
@HiltAndroidApp
class TodoApplication : Application(), Configuration.Provider {

    // @HiltWorker の Worker を作る工場。Hilt が super.onCreate() の中で注入する
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var backupScheduler: TodoBackupScheduler

    // 起動のたびに、毎日のバックアップを予約する（同じ名前の予約があれば何もしない）。super.onCreate() で注入が終わるので、その後で使う
    override fun onCreate() {
        super.onCreate()
        backupScheduler.schedulePeriodicBackup()
    }

    // get() にして、読まれるたびに作る（注入より前に読まれないことは、WorkManager を onCreate 以降で使うことで守る）
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            // WorkManager のログ（タグは WM- で始まる）。INFO なら Worker の結果が出る。DEBUG にすると、さらに詳しく出る
            .setMinimumLoggingLevel(Log.INFO)
            .build()
}
