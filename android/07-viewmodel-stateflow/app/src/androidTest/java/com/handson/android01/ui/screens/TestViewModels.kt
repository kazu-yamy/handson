package com.handson.android01.ui.screens

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.handson.android01.kotlin.Todo

// テスト用: 初期データを指定して ViewModel を作る。待ち時間は 0 にして、最初の描画から一覧が出ているようにする。
// viewModel(factory = ...) は、同じ ViewModelStoreOwner（ここでは Activity）の中では同じインスタンスを返す
@Composable
fun testViewModel(initialTodos: List<Todo> = demoTodoList, loadDelayMillis: Long = 0L): TodoListViewModel =
    viewModel(factory = viewModelFactory { initializer { TodoListViewModel(createSavedStateHandle(), initialTodos, loadDelayMillis) } })
