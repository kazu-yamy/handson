package com.handson.android01.ui.screens

import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import com.handson.android01.kotlin.Todo
import kotlinx.coroutines.launch

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

    // 追加用ボトムシートの表示フラグ。回転しても開いたままにする
    var showAddSheet by rememberSaveable { mutableStateOf(false) }

    // 絞り込みの選択。画面回転のあとも残すので rememberSaveable（enum は Serializable なのでそのまま保存できる）
    var filter by rememberSaveable { mutableStateOf(TodoFilter.All) }

    // Snackbar の状態と、イベントから呼ぶためのコルーチンスコープ
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    TodoListScreen(
        todos = todos,
        snackbarHostState = snackbarHostState,
        filter = filter,
        onFilterChange = { filter = it },
        onDelete = { todo ->
            val index = todos.indexOf(todo)
            if (index >= 0) {
                todos.removeAt(index)
                // 表示中の Snackbar があれば閉じてから出す（出さないと、前の Snackbar が消えるまで待たされる）
                snackbarHostState.currentSnackbarData?.dismiss()
                // 削除はユーザーの操作（イベント）から起きるので、LaunchedEffect ではなく scope.launch で出す。
                // showSnackbar は Snackbar が消えるまで中断し、押されたボタンを返す
                scope.launch {
                    val result = snackbarHostState.showSnackbar(
                        message = "「${todo.title}」を削除しました",
                        actionLabel = "元に戻す",
                        duration = SnackbarDuration.Short,
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        // 削除前の位置に戻す（その間に件数が減っていても範囲内に収める）
                        todos.add(index.coerceAtMost(todos.size), todo)
                    }
                }
            }
        },
        // 追加の入口は FAB。押すと入力用のボトムシートを開く
        inputState = inputState,
        onFabClick = { showAddSheet = true },
        showAddSheet = showAddSheet,
        onAddSheetDismiss = { showAddSheet = false },
        onDeleteDone = {
            val count = todos.count { it.done }
            todos.removeAll { it.done }
            snackbarHostState.currentSnackbarData?.dismiss()
            scope.launch { snackbarHostState.showSnackbar("完了済み $count 件を削除しました") }
        },
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
