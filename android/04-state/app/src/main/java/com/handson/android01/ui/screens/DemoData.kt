package com.handson.android01.ui.screens

import com.handson.android01.kotlin.Todo

// 画面に出すための固定データ（02 の demoTodos と同じ内容）。ステップ 5 以降の Preview とテストでも使う
val demoTodoList: List<Todo> = listOf(
    Todo(1, "learn kotlin basics", done = true),
    Todo(2, "build compose screen"),
    Todo(3, "write unit tests", done = true),
)
