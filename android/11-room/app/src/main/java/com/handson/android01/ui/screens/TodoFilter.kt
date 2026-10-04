package com.handson.android01.ui.screens

import com.handson.android01.kotlin.Todo

// 一覧の絞り込み。enum は Serializable なので、rememberSaveable にそのまま入れられる
enum class TodoFilter(val label: String) {
    All("すべて"),
    Active("未完了"),
    Done("完了"),
}

// 絞り込みの結果に、そのセクションの Todo が含まれるか
fun TodoFilter.showsActive(): Boolean = this != TodoFilter.Done
fun TodoFilter.showsDone(): Boolean = this != TodoFilter.Active

// 一覧が空のときに出す文言
fun TodoFilter.emptyMessage(): String = when (this) {
    TodoFilter.All -> "Todo はありません"
    TodoFilter.Active -> "未完了の Todo はありません"
    TodoFilter.Done -> "完了した Todo はありません"
}

fun List<Todo>.activeTodos(): List<Todo> = filter { !it.done }
fun List<Todo>.doneTodos(): List<Todo> = filter { it.done }
