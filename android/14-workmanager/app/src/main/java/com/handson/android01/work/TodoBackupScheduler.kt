package com.handson.android01.work

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject

// バックアップの予約の窓口。WorkManager への登録をここにまとめる
class TodoBackupScheduler @Inject constructor(@ApplicationContext private val context: Context) {

    // WorkManager.getInstance は、初回に TodoApplication.workManagerConfiguration を読んで初期化する。
    // コンストラクタで取ると、TodoApplication への注入の途中で読まれる。今は workerFactory が先に注入されるので問題ないが、
    // フィールドの宣言の順に頼ることになるので、使うときに取る
    private val workManager: WorkManager get() = WorkManager.getInstance(context)

    // 1 日に 1 回のバックアップ。周期は「この間に 1 回」で、時刻は決まらない（Doze などで後ろにずれる）。最短は 15 分。
    // 起動のたびに呼ぶが、同じ名前の予約があれば何もしない（KEEP）。CANCEL_AND_REENQUEUE（旧 REPLACE）にすると、起動のたびに周期が最初からになる
    fun schedulePeriodicBackup() {
        val request = PeriodicWorkRequestBuilder<TodoBackupWorker>(1, TimeUnit.DAYS)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        workManager.enqueueUniquePeriodicWork(UNIQUE_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    companion object {
        const val UNIQUE_NAME = "todo-backup"

        // ネットワークにつながっていて、電池が少なくないときだけ動かす。満たさないあいだは待ち、満たした時点で動く。
        // 実行中に満たさなくなると止められ（doWork のコルーチンがキャンセルされる）、後でやり直される
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()
    }
}
