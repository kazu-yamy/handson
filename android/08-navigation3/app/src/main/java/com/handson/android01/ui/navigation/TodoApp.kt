package com.handson.android01.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.handson.android01.ui.screens.TodoDetailPage
import com.handson.android01.ui.screens.TodoListPage
import com.handson.android01.ui.screens.TodoListViewModel

// アプリ全体の画面遷移。バックスタックは「キーを並べただけのリスト」で、自分で持つ。
// 進む = リストに追加、戻る = 末尾を取り除く。NavDisplay は、末尾のキーに対応する画面を表示する
@Composable
fun TodoApp(
    modifier: Modifier = Modifier,
    // NavDisplay の外（Activity が持ち主の ViewModelStoreOwner）で作るので、一覧と詳細で同じインスタンスを使える
    viewModel: TodoListViewModel = viewModel(),
) {
    // rememberNavBackStack: remember と同じ使い方で、画面回転とプロセスの再生成のあとも中身が戻るバックスタックを作る。
    // 返るのは NavBackStack<NavKey>（MutableList なので、add と removeLastOrNull はこれまでどおり）
    val backStack = rememberNavBackStack(TodoList)

    NavDisplay(
        backStack = backStack,
        modifier = modifier,
        // システムの戻る操作（ジェスチャー・戻るボタン）で呼ばれる。末尾を取り除くと、1 つ前の画面が表示される
        onBack = { backStack.removeLastOrNull() },
        // entryProvider: キーから「画面（NavEntry）」を作る対応表。entry<キーの型> { key -> 画面 } と書く
        entryProvider = entryProvider {
            entry<TodoList> {
                TodoListPage(
                    viewModel = viewModel,
                    onOpenDetail = { id -> backStack.add(TodoDetail(id)) },
                )
            }
            entry<TodoDetail> { key ->
                TodoDetailPage(
                    todoId = key.id,
                    viewModel = viewModel,
                    onBack = { backStack.removeLastOrNull() },
                )
            }
        },
    )
}
