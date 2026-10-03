package com.handson.android01.kotlin

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

// Step 6: コルーチンの入口

// suspend fun: 途中で中断・再開できる関数。delay は待ち時間の間スレッドを塞がない
suspend fun fetchTodos(): List<Todo> {
    delay(1000) // 通信の代わりに 1 秒待つ
    return sampleTodos()
}

// withContext(Dispatchers.IO): ブロッキングになりうる処理を IO 用のスレッドで実行する
suspend fun fetchTodosOnIo(): List<Todo> = withContext(Dispatchers.IO) {
    delay(1000)
    sampleTodos()
}

// async / await: 2 つの処理を並列に走らせ、両方の結果を待つ
suspend fun fetchTodosAndCount(): Pair<List<Todo>, Int> = coroutineScope {
    val todos = async { fetchTodos() }
    val count = async {
        delay(1000)
        4
    }
    todos.await() to count.await()
}
