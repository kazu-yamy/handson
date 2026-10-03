package com.handson.android01.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.handson.android01.kotlin.Todo
import com.handson.android01.ui.components.CountBadge
import com.handson.android01.ui.components.TodoInput
import com.handson.android01.ui.components.TodoRow
import com.handson.android01.ui.theme.HandsonAndroid01Theme
import kotlinx.coroutines.launch

// TopAppBar は Material3 1.4 時点で実験的 API。使うには @OptIn が必要（未指定だとコンパイルエラー）
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoListScreen(
    todos: List<Todo>,
    inputState: TextFieldState,
    onToggle: (id: Int, done: Boolean) -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onDelete: (Todo) -> Unit = {},
    onFabClick: () -> Unit = {},
) {
    // rememberLazyListState: スクロール位置を覚える。LazyColumn に渡すと、位置を読んだり動かしたりできる
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    // derivedStateOf: firstVisibleItemIndex はスクロールのたびに変わるが、知りたいのは「先頭から離れたか」だけ。
    // 結果の Boolean が変わったときにだけ、これを読む側が再コンポーズされる
    val showToTop by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 } }

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
        // snackbarHost: Snackbar の表示場所。状態（SnackbarHostState）は呼び出し側が持ち、showSnackbar で表示を依頼する
        snackbarHost = { SnackbarHost(snackbarHostState) },
        // floatingActionButton: 右下に浮かぶボタンの置き場所。2 つ並べたいときは Column に入れる
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (showToTop) {
                    SmallFloatingActionButton(
                        onClick = { scope.launch { listState.animateScrollToItem(0) } },
                        modifier = Modifier.testTag("scroll_to_top"),
                    ) {
                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = "先頭へ")
                    }
                }
                ExtendedFloatingActionButton(
                    onClick = onFabClick,
                    icon = { Icon(Icons.Default.Add, contentDescription = "追加") },
                    text = { Text("追加") },
                    modifier = Modifier.testTag("fab_add"),
                )
            }
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

            // LazyColumn: 画面に見えている行だけを compose する遅延リスト。残りの高さを weight(1f) で使い切る。
            // 入力欄を下に置くと、キーボードが出たとき画面全体が押し上げられるので、リストを最後に置く
            // key に id を渡すと、並べ替えや削除のあとも、行と状態の対応が id で保たれる
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .testTag("todo_list"),
                // 下の余白は、FAB が最後の行に重なって押せなくならないように広げる
                contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(todos, key = { it.id }) { todo ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        TodoRow(
                            todo = todo,
                            onToggle = { done -> onToggle(todo.id, done) },
                            onDelete = { onDelete(todo) },
                        )
                    }
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
