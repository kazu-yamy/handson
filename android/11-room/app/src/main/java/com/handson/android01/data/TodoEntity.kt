package com.handson.android01.data

import androidx.room3.Entity
import androidx.room3.PrimaryKey
import com.handson.android01.kotlin.Todo

// DB の 1 行（todos テーブルの 1 行）。画面で使う Todo とは別の型にして、DB の都合（autoGenerate など）を画面に持ち込まない。
// id = 0 のまま insert すると、Room が新しい id を振る
@Entity(tableName = "todos")
data class TodoEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val done: Boolean,
)

fun TodoEntity.toTodo(): Todo = Todo(id = id, title = title, done = done)

fun Todo.toEntity(): TodoEntity = TodoEntity(id = id, title = title, done = done)
