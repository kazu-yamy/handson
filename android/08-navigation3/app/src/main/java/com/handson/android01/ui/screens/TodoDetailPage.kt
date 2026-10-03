package com.handson.android01.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.handson.android01.kotlin.Todo

// 詳細画面の stateful な薄いラッパー。一覧と同じ ViewModel（呼び出し側から渡される）の状態から、id の Todo を探して渡す
@Composable
fun TodoDetailPage(
    todoId: Int,
    viewModel: TodoListViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // 最後に見つかった Todo を覚えておく。削除して一覧へ戻るアニメーションの間も、
    // この画面は残っているため、見つからなくなった後はこれを表示する
    var lastFound by remember { mutableStateOf<Todo?>(null) }
    val found = uiState.todos.find { it.id == todoId }
    SideEffect { if (found != null) lastFound = found }
    TodoDetailScreen(
        todo = found ?: lastFound,
        onBack = onBack,
        onToggle = { done -> viewModel.toggleTodo(todoId, done) },
        // 削除して、一覧へ戻る。データは一覧と同じ ViewModel にあるので、戻った一覧からもこの行が消えている
        onDelete = {
            viewModel.deleteTodo(todoId)
            onBack()
        },
        modifier = modifier,
    )
}
