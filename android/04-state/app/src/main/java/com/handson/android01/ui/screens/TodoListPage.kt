package com.handson.android01.ui.screens

import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import com.handson.android01.kotlin.Todo

// stateful（状態を持つ）な画面。Todo の一覧は、ここで mutableStateList として持つ
// rememberSaveable: 画面回転のあとも、チェックの状態が残るようにする。要素の Todo は @Parcelize なので、一覧ごと保存できる
@Composable
fun TodoListPage(
    initialTodos: List<Todo>,
    modifier: Modifier = Modifier,
) {
    val todos = rememberSaveable {
        initialTodos.toMutableStateList()
    }

    // rememberTextFieldState: 入力中の文字列の状態を作る。画面回転後も入力の内容は残る
    val inputState = rememberTextFieldState()

    TodoListScreen(
        todos = todos,
        inputState = inputState,
        onToggle = { id, done ->
            // 完了状態を変えるのは状態の持ち主（ここ）の役目。要素を新しい Todo に置き換えると、読んでいる画面が再コンポーズされる
            val index = todos.indexOfFirst { it.id == id }
            if (index >= 0) {
                todos[index] = todos[index].copy(done = done)
            }
        },
        onAddClick = {
            val title = inputState.text.toString().trim()
            if (title.isNotEmpty()) {
                // id は今ある最大値 + 1。追加したら入力欄を空に戻す
                todos.add(Todo(id = (todos.maxOfOrNull { it.id } ?: 0) + 1, title = title))
                inputState.clearText()
            }
        },
        modifier = modifier,
    )
}
