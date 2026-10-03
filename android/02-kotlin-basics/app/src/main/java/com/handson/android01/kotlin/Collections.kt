package com.handson.android01.kotlin

// Step 4: コレクションとラムダ

// List は読み取り専用（要素の追加・削除はできない）
fun sampleTodos(): List<Todo> = listOf(
    Todo(1, "買い物", done = true),
    Todo(2, "掃除"),
    Todo(3, "Kotlin を学ぶ", done = true),
    Todo(4, "散歩"),
)

// MutableList は可変（追加・削除ができる）
fun collectTitles(todos: List<Todo>): MutableList<String> {
    val result = mutableListOf<String>()
    for (todo in todos) {
        result.add(todo.title)
    }
    return result
}

// it: ラムダの引数が 1 つのときの暗黙の名前
fun titlesOfDone(todos: List<Todo>): List<String> =
    todos.filter { it.done }.map { it.title }

// 明示的なラムダ引数: 引数が複数あるときは名前を付ける
fun sortedByIdDescending(todos: List<Todo>): List<Todo> =
    todos.sortedBy { todo -> -todo.id }

// groupBy: キーごとに要素をまとめた Map を作る
fun groupByDone(todos: List<Todo>): Map<Boolean, List<Todo>> =
    todos.groupBy { it.done }

// firstOrNull: 条件に合う最初の要素。無ければ null
fun findTodo(todos: List<Todo>, id: Int): Todo? =
    todos.firstOrNull { it.id == id }

// 末尾ラムダ: 最後の引数がラムダなら、括弧の外に書ける
fun countWhere(todos: List<Todo>, predicate: (Todo) -> Boolean): Int =
    todos.count(predicate)
