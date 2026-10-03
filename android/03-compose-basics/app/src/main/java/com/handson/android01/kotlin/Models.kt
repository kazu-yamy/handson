package com.handson.android01.kotlin

// Step 3: データクラス・sealed interface・enum class

// data class: equals / hashCode / toString / copy / 分解宣言が自動で生成される
data class Todo(
    val id: Int,
    val title: String,
    val done: Boolean = false,
)

// enum class: 決まった値の集合。when で網羅的に扱える
enum class Priority { LOW, MEDIUM, HIGH }

// sealed interface: 実装クラスを同じモジュール・同じパッケージ内に限定できる（Kotlin 1.5 以降）。
// when で全ての子型を書けば、else なしでも網羅性がコンパイル時に検査される
sealed interface UiState {
    data object Loading : UiState
    data class Success(val data: List<Todo>) : UiState
    data class Error(val message: String) : UiState
}

fun stateLabel(state: UiState): String = when (state) {
    is UiState.Loading -> "読み込み中"
    is UiState.Success -> "${state.data.size} 件"
    is UiState.Error -> "エラー: ${state.message}"
}

fun priorityLabel(priority: Priority): String = when (priority) {
    Priority.LOW -> "低"
    Priority.MEDIUM -> "中"
    Priority.HIGH -> "高"
}
