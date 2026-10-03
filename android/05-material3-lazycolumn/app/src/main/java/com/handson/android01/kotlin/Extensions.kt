package com.handson.android01.kotlin

// Step 5: 拡張関数とスコープ関数

// 拡張関数: 既存の型に、クラスを継承せずに関数を足す。レシーバーは this で参照する
fun String.toTitleCase(): String =
    split(" ").joinToString(" ") { word -> word.replaceFirstChar { it.uppercaseChar() } }

// List<Todo> に対する拡張関数。Todo のリストを加工する
fun List<Todo>.doneCount(): Int = count { it.done }

// 表示用の 1 行にまとめる（MainActivity の画面で使う）
fun List<Todo>.summaryLine(): String = "${size} 件中 ${doneCount()} 件完了"

// 組み立て用の可変クラス（Todo は data class で不変にしたので、初期化の例として別に用意）
class TodoDraft {
    var title: String = ""
    var done: Boolean = false
}

// apply: レシーバーを初期化し、レシーバー自身を返す（オブジェクトの設定）
fun draftWithApply(title: String): TodoDraft = TodoDraft().apply {
    this.title = title
    done = false
}

// also: 副作用（ログ・追加処理）を入れ、レシーバー自身を返す
fun addAndRecord(list: MutableList<String>, title: String): MutableList<String> =
    list.also { it.add(title) }

// run: レシーバーを使って結果の値を計算する（this でレシーバーを参照）
fun summarizeWithRun(todos: List<Todo>): String = todos.run {
    "${size} 件中 ${doneCount()} 件完了"
}

// with: レシーバーを引数で渡し、ブロックの結果を返す（拡張関数ではなく通常の関数）
fun summarizeWithWith(todos: List<Todo>): String = with(todos) {
    "${size} 件中 ${doneCount()} 件完了"
}
