package com.handson.android01.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.handson.android01.kotlin.Todo
import com.handson.android01.ui.components.CountBadge
import com.handson.android01.ui.components.TodoInput
import com.handson.android01.ui.components.TodoRow
import com.handson.android01.ui.theme.HandsonAndroid01Theme

// TopAppBar は Material3 1.4 時点で実験的 API。使うには @OptIn が必要（未指定だとコンパイルエラー）
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoListScreen(
    todos: List<Todo>,
    inputState: TextFieldState,
    onToggle: (id: Int, done: Boolean) -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Scaffold: TopAppBar・本文・FAB などの置き場所を用意する Material3 の土台。
    // 本文には innerPadding を必ず適用し、TopAppBar の下に隠れないようにする
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Todo") },
                actions = { CountBadge(count = todos.count { !it.done }) },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // derivedStateOf: todos から計算する完了件数を State として覚える。remember(todos) のキーは、todos の参照が変わったら計算式を作り直すため
            val doneCount by remember(todos) { derivedStateOf { todos.count { it.done } } }
            Text(
                text = "${todos.size} 件中 $doneCount 件完了",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.titleLarge,
            )

            // Card の中に行を並べ、行の間だけ区切り線を入れる
            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Column {
                    todos.forEachIndexed { index, todo ->
                        TodoRow(
                            todo = todo,
                            onToggle = { done -> onToggle(todo.id, done) },
                        )
                        if (index < todos.lastIndex) {
                            HorizontalDivider()
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 入力欄と追加ボタン。入力の文字列は状態の持ち主（TodoListPage）が持つ
            TodoInput(state = inputState, onAddClick = onAddClick)

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // 以下 2 つはボタンの見た目の違いを示すための置き場。処理は後の項目で実装する
                OutlinedButton(onClick = {}) {
                    Text("フィルタ")
                }
                TextButton(onClick = {}) {
                    Text("閉じる")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TodoListScreenPreview() {
    HandsonAndroid01Theme {
        TodoListScreen(todos = demoTodoList, inputState = rememberTextFieldState(), onToggle = { _, _ -> }, onAddClick = {})
    }
}
