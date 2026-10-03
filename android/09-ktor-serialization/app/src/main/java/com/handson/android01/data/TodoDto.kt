package com.handson.android01.data

import com.handson.android01.kotlin.Todo
import kotlinx.serialization.Serializable

// API が返す JSON の 1 件分の形（https://jsonplaceholder.typicode.com/todos の要素）。
// アプリ内で使う Todo とは別の型にして、API の都合（userId など）をアプリの中に持ち込まない。
// @Serializable: Kotlin のシリアライズプラグインが、JSON との変換コードをコンパイル時に生成する
@Serializable
data class TodoDto(
    val userId: Int,
    val id: Int,
    val title: String,
    val completed: Boolean,
)

// API の形からアプリの Todo への変換
fun TodoDto.toTodo(): Todo = Todo(id = id, title = title, done = completed)
