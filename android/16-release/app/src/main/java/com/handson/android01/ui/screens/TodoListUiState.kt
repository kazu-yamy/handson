package com.handson.android01.ui.screens

import com.handson.android01.data.TodoFilter
import com.handson.android01.kotlin.Todo

// 一覧画面の「画面状態」。ViewModel が持ち、UI は読むだけ。
// val だけの data class（immutable）にして、変更は copy で新しい値を作る。StateFlow は値が変わったとき（equals で比べる）だけ通知する
data class TodoListUiState(
    val todos: List<Todo> = emptyList(),
    val filter: TodoFilter = TodoFilter.All,
    // 読み込み中かどうか。true のあいだ、画面は一覧の代わりに読み込み表示を出す
    val isLoading: Boolean = false,
    // 読み込みに失敗したときの、画面に出すメッセージ。null なら失敗していない。
    // 失敗中（isLoading = false かつ errorMessage != null）は、一覧の代わりにエラー表示と「再試行」を出す
    val errorMessage: String? = null,
    // 最後にバックアップした時刻（エポックミリ秒）。null はまだ一度も無い
    val lastBackupAt: Long? = null,
)
