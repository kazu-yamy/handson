package com.handson.android01.data

// 一覧の絞り込み。
// name は DataStore に保存する値なので、名前を変えない。表示名は画面側（ui.screens）で決める
enum class TodoFilter {
    All,
    Active,
    Done,
}
