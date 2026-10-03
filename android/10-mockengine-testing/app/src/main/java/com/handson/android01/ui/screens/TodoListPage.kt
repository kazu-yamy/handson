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
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

// stateful（状態を持つ）な画面。Todo の一覧と絞り込みは ViewModel が持ち、ここは購読して画面に渡す。
// シートの表示・入力中の文字・Snackbar など「UI だけの状態」は、これまでどおりこの Composable に残す
@Composable
fun TodoListPage(
    modifier: Modifier = Modifier,
    viewModel: TodoListViewModel = viewModel(),
    // 行のタイトルが押されたときに呼ばれる。どこへ進むかは、呼び出し側（画面遷移）が決める
    onOpenDetail: (id: Int) -> Unit = {},
) {
    // collectAsStateWithLifecycle: StateFlow を Compose の State として読む。画面が STARTED より下（バックグラウンド）のあいだは収集を止める
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // rememberTextFieldState: 入力中の文字列の状態を作る。画面回転後も入力の内容は残る
    val inputState = rememberTextFieldState()

    // 追加用ボトムシートの表示フラグ。UI だけの状態なので Composable に残し、回転しても開いたままにする
    var showAddSheet by rememberSaveable { mutableStateOf(false) }

    // Snackbar の状態と、イベントから呼ぶためのコルーチンスコープ
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    TodoListScreen(
        todos = uiState.todos,
        isLoading = uiState.isLoading,
        errorMessage = uiState.errorMessage,
        onRetry = viewModel::retry,
        snackbarHostState = snackbarHostState,
        filter = uiState.filter,
        onFilterChange = viewModel::setFilter,
        onDelete = { todo ->
            val index = uiState.todos.indexOf(todo)
            if (index >= 0) {
                viewModel.deleteTodo(todo.id)
                // 表示中の Snackbar があれば閉じてから出す（出さないと、前の Snackbar が消えるまで待たされる）
                snackbarHostState.currentSnackbarData?.dismiss()
                // Snackbar を出すのは UI の仕事なので、ここ（Composable）に残す。
                // showSnackbar は Snackbar が消えるまで中断し、押されたボタンを返す
                scope.launch {
                    val result = snackbarHostState.showSnackbar(
                        message = "「${todo.title}」を削除しました",
                        actionLabel = "元に戻す",
                        duration = SnackbarDuration.Short,
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.restoreTodo(todo, index)
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
            val count = uiState.todos.count { it.done }
            viewModel.deleteDone()
            snackbarHostState.currentSnackbarData?.dismiss()
            scope.launch { snackbarHostState.showSnackbar("完了済み $count 件を削除しました") }
        },
        onToggle = viewModel::toggleTodo,
        onAddClick = {
            val title = inputState.text.toString()
            if (title.isNotBlank()) {
                viewModel.addTodo(title)
                inputState.clearText()
            }
        },
        onOpenDetail = onOpenDetail,
        modifier = modifier,
    )
}
