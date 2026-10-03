package com.handson.android01.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    filter: TodoFilter = TodoFilter.All,
    onFilterChange: (TodoFilter) -> Unit = {},
    showAddSheet: Boolean = false,
    onAddSheetDismiss: () -> Unit = {},
    onDeleteDone: () -> Unit = {},
    isLoading: Boolean = false,
    onOpenDetail: (id: Int) -> Unit = {},
) {
    // rememberLazyListState: スクロール位置を覚える。LazyColumn に渡すと、位置を読んだり動かしたりできる
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    // derivedStateOf: firstVisibleItemIndex はスクロールのたびに変わるが、知りたいのは「先頭から離れたか」だけ。
    // 結果の Boolean が変わったときにだけ、これを読む側が再コンポーズされる
    val showToTop by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 } }

    // 確認ダイアログを出すかどうか。画面回転のあとも開いたままにするので rememberSaveable
    var showDeleteDoneDialog by rememberSaveable { mutableStateOf(false) }

    // Scaffold: TopAppBar・本文・FAB などの置き場所を用意する Material3 の土台。
    // 本文には innerPadding を必ず適用し、TopAppBar の下に隠れないようにする
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Todo") },
                actions = {
                    // 読み込み中は、一括削除も件数バッジも出さない（操作できず、「0 件」と誤解させるため）
                    if (!isLoading) {
                        // 完了済みをまとめて削除する。取り消せない操作なので、確認ダイアログを挟む
                        IconButton(
                            onClick = { showDeleteDoneDialog = true },
                            enabled = todos.any { it.done },
                            modifier = Modifier.testTag("delete_done_button"),
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "完了済みをすべて削除")
                        }
                        CountBadge(count = todos.count { !it.done })
                    }
                },
            )
        },
        // snackbarHost: Snackbar の表示場所。状態（SnackbarHostState）は呼び出し側が持ち、showSnackbar で表示を依頼する
        snackbarHost = { SnackbarHost(snackbarHostState) },
        // floatingActionButton: 右下に浮かぶボタンの置き場所。2 つ並べたいときは Column に入れる
        floatingActionButton = {
            // 読み込み中は、読み込み表示だけを出す。追加すると、読み込み後の上書きで消えてしまう
            if (!isLoading) Column(
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
            if (isLoading) {
                // 読み込み中: サマリ・フィルタ・一覧は出さず、読み込み表示だけにする（「0 件中 0 件完了」や空状態の文言は、「何も無い」と誤解させる）
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .testTag("loading"),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
                return@Column
            }

            // derivedStateOf: todos から計算する完了件数を State として覚える。remember(todos) のキーは、todos の参照が変わったら計算式を作り直すため
            val doneCount by remember(todos) { derivedStateOf { todos.count { it.done } } }
            Text(
                text = "${todos.size} 件中 $doneCount 件完了",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.titleLarge,
            )

            // FilterChip: 選択中かどうかを持つ小さな絞り込みボタン。選択は呼び出し側の状態（filter）が決める
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TodoFilter.entries.forEach { option ->
                    FilterChip(
                        selected = filter == option,
                        onClick = { onFilterChange(option) },
                        label = { Text(option.label) },
                        modifier = Modifier.testTag("filter_${option.name}"),
                    )
                }
            }

            val activeTodos = todos.activeTodos()
            val doneTodos = todos.doneTodos()
            val visibleCount =
                (if (filter.showsActive()) activeTodos.size else 0) + (if (filter.showsDone()) doneTodos.size else 0)

            if (visibleCount == 0) {
                // 空状態: 何も無いことと、その理由を伝える
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .testTag("empty_state"),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = filter.emptyMessage(),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                return@Column
            }

            // LazyColumn: 画面に見えている行だけを compose する遅延リスト。残りの高さを weight(1f) で使い切る。
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
                // stickyHeader: スクロールしても、次のセクションが来るまで画面の上端に貼り付く見出し。
                // 見出しにも、行と重ならない key を付ける（Int の id と区別するため文字列にする）
                if (filter.showsActive() && activeTodos.isNotEmpty()) {
                    stickyHeader(key = "header_active") { SectionHeader("未完了", Modifier.testTag("header_active")) }
                    items(activeTodos, key = { it.id }) { todo ->
                        SwipeableTodoRow(todo, onToggle, onDelete, onOpenDetail, Modifier.animateItem())
                    }
                }
                if (filter.showsDone() && doneTodos.isNotEmpty()) {
                    stickyHeader(key = "header_done") { SectionHeader("完了", Modifier.testTag("header_done")) }
                    items(doneTodos, key = { it.id }) { todo ->
                        SwipeableTodoRow(todo, onToggle, onDelete, onOpenDetail, Modifier.animateItem())
                    }
                }
            }
        }
    }

    if (showDeleteDoneDialog) {
        val doneCount = todos.count { it.done }
        AlertDialog(
            onDismissRequest = { showDeleteDoneDialog = false },
            title = { Text("完了済みをすべて削除") },
            text = { Text("完了済みの Todo $doneCount 件を削除します。この操作は元に戻せません。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteDone()
                        showDeleteDoneDialog = false
                    },
                    modifier = Modifier.testTag("confirm_delete_done"),
                ) { Text("削除") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDoneDialog = false }) { Text("キャンセル") }
            },
        )
    }

    // ModalBottomSheet: 画面の下から出る入力用のシート。出す・出さないは showAddSheet（呼び出し側の状態）で決める。
    // skipPartiallyExpanded = true: 「半分だけ開いた」状態を飛ばして、開くときは最初から全部開く
    if (showAddSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        // 閉じるときは、先に sheetState.hide() でシートを下げ終えてから表示フラグを落とす（先に落とすと、下がるアニメーションを待たずにシートがコンポジションから外れる）
        val closeSheet: () -> Unit = {
            scope.launch { sheetState.hide() }.invokeOnCompletion {
                if (!sheetState.isVisible) onAddSheetDismiss()
            }
        }
        ModalBottomSheet(
            // 背景のタップ・下へのドラッグ・戻るボタンで呼ばれる（hide のアニメーションは済んでいる）
            onDismissRequest = onAddSheetDismiss,
            sheetState = sheetState,
            modifier = Modifier.testTag("add_sheet"),
        ) {
            Text(
                text = "新しい Todo",
                modifier = Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.titleLarge,
            )
            // 入力欄と追加ボタン（04 では画面の中に置いていた部品）。入力の文字列は状態の持ち主（TodoListPage）が持つ
            TodoInput(
                state = inputState,
                onAddClick = {
                    onAddClick()
                    closeSheet()
                },
                modifier = Modifier.padding(vertical = 16.dp),
            )
        }
    }
}

// 貼り付く見出し。背景を塗らないと、後ろを通る行が透けて見える
@Composable
private fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = 8.dp),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
    )
}

// 左へスワイプして削除できる行。LazyItemScope の animateItem は、呼び出し側から modifier で受け取る
@Composable
private fun SwipeableTodoRow(
    todo: Todo,
    onToggle: (id: Int, done: Boolean) -> Unit,
    onDelete: (Todo) -> Unit,
    onOpenDetail: (id: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    // 行ごとに、スワイプの状態を持つ。key があるので、行を消しても他の行の状態とは混ざらない
    val dismissState = rememberSwipeToDismissBoxState()
    val rowScope = rememberCoroutineScope()
    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        // 右へのスワイプは使わず、左へのスワイプ（EndToStart）だけで削除する
        enableDismissFromStartToEnd = false,
        // 確定（しきい値を超えて動きが止まった）ときに呼ばれる。削除は状態の持ち主に任せる。
        // rememberSwipeToDismissBoxState は rememberSaveable なので、「消えた」状態が同じ key で保存される。
        // そのままだと「元に戻す」で戻した行が、再び消えた状態で復元され、すぐ削除し直される。
        // 先に snapTo で状態を Settled に戻してから削除する
        onDismiss = {
            rowScope.launch {
                dismissState.snapTo(SwipeToDismissBoxValue.Settled)
                onDelete(todo)
            }
        },
        backgroundContent = {
            // 動かしている間だけ、赤い背景とゴミ箱を出す
            if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CardDefaults.shape)
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .padding(end = 24.dp),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }
        },
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            TodoRow(
                todo = todo,
                onToggle = { done -> onToggle(todo.id, done) },
                onDelete = { onDelete(todo) },
                onOpen = { onOpenDetail(todo.id) },
            )
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
