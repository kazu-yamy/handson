package com.handson.android01.ui.screens

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.handson.android01.data.SettingsRepository
import com.handson.android01.data.TodoApi
import com.handson.android01.data.TodoRepository
import com.handson.android01.kotlin.Todo

// テスト用: 偽の DAO（FakeTodoDao）・偽のサーバー（FakeTodoServer）・偽の DataStore（FakeDataStore）につないだ ViewModel を作る。api を渡さなければ、initialTodos をすぐに返すサーバーを使う。
// viewModel(factory = ...) は、同じ ViewModelStoreOwner（ここでは Activity）の中では同じインスタンスを返す
@Composable
fun testViewModel(initialTodos: List<Todo> = demoTodoList, api: TodoApi? = null): TodoListViewModel =
    viewModel(
        factory = viewModelFactory {
            initializer {
                val settings = SettingsRepository(FakeDataStore())
                TodoListViewModel(TodoRepository(FakeTodoDao(), api ?: FakeTodoServer(initialTodos).api, settings), settings)
            }
        },
    )
