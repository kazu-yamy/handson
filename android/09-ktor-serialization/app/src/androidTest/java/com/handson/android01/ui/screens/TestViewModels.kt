package com.handson.android01.ui.screens

import androidx.compose.runtime.Composable
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.handson.android01.data.TodoApi
import com.handson.android01.kotlin.Todo

// テスト用: 偽のサーバー（FakeTodoServer）につないだ ViewModel を作る。api を渡さなければ、initialTodos をすぐに返すサーバーを使う。
// viewModel(factory = ...) は、同じ ViewModelStoreOwner（ここでは Activity）の中では同じインスタンスを返す
@Composable
fun testViewModel(initialTodos: List<Todo> = demoTodoList, api: TodoApi? = null): TodoListViewModel =
    viewModel(
        factory = viewModelFactory {
            initializer { TodoListViewModel(createSavedStateHandle(), api ?: FakeTodoServer(initialTodos).api) }
        },
    )
