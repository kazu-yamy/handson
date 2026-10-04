package com.handson.android01.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.handson.android01.data.TodoRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.ktor.client.plugins.ClientRequestException
import kotlin.coroutines.cancellation.CancellationException

// バックグラウンドで 1 回分のバックアップを行う Worker。
// @HiltWorker + @AssistedInject: Context と WorkerParameters は WorkManager が実行のたびに渡し（@Assisted）、
// それ以外（TodoRepository）は Hilt が組み立てて渡す。作るのは TodoApplication で設定した HiltWorkerFactory
@HiltWorker
class TodoBackupWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val repository: TodoRepository,
) : CoroutineWorker(appContext, params) {

    // CoroutineWorker の doWork はメインスレッドの外（既定では Dispatchers.Default）で動く。
    // 戻り値で、WorkManager に結果を伝える（success / failure / retry）
    override suspend fun doWork(): Result = try {
        repository.backupTodos()
        Result.success()
    } catch (e: CancellationException) {
        // 条件を満たさなくなったときなど、WorkManager に止められると、コルーチンがキャンセルされる。握りつぶさない
        throw e
    } catch (e: ClientRequestException) {
        // 4xx: 送り方が間違っている。何度送っても同じなので、やり直さない
        Result.failure()
    } catch (e: Exception) {
        // 通信の失敗・5xx・タイムアウト: 時間をおけば成功するかもしれない。
        // retry を返すと、バックオフ（30 秒 → 60 秒 → 120 秒…）の後にもう一度実行される。上限を超えたらあきらめる
        if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.failure()
    }

    companion object {
        const val MAX_RETRIES = 3
    }
}
