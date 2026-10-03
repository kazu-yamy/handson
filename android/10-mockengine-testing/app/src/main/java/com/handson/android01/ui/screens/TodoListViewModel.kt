package com.handson.android01.ui.screens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.handson.android01.data.AppContainer
import com.handson.android01.data.TodoApi
import com.handson.android01.kotlin.Todo
import io.ktor.client.call.NoTransformationFoundException
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ResponseException
import io.ktor.serialization.ContentConvertException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import java.io.IOException

// 画面状態を持つ ViewModel。画面回転（Activity の作り直し）では破棄されず、同じインスタンスが使われ続ける。
// SavedStateHandle: プロセスが終了されても残したい値の入れ物。Activity の onSaveInstanceState の Bundle に保存され、
// プロセスが作り直されたときに、保存した値が入った状態で渡される
class TodoListViewModel(
    private val savedStateHandle: SavedStateHandle,
    // Todo の取得元。テストでは、通信しない MockEngine 入りの TodoApi を渡す
    private val api: TodoApi,
) : ViewModel() {

    // viewModel() の既定のファクトリは、引数なし、または SavedStateHandle だけのコンストラクタを探して呼ぶ。
    // そのため、アプリでは TodoApi を AppContainer（手書きの置き場所）から受け取る
    constructor(savedStateHandle: SavedStateHandle) : this(savedStateHandle, AppContainer.todoApi)

    // 書き込めるのは ViewModel の中だけ。外には読み取り専用の StateFlow として公開する。
    // 保存されていればそれを使う（読み込み済みなので isLoading は false で、取得し直さない）。無ければ空の一覧と isLoading = true で始め、下の init で読み込む。
    // ArrayList<Todo> は、要素が Parcelable なので保存できる
    private val restoredTodos = savedStateHandle.get<ArrayList<Todo>>(KEY_TODOS)
    private val _uiState = MutableStateFlow(
        TodoListUiState(
            todos = restoredTodos ?: emptyList(),
            filter = savedStateHandle.get<TodoFilter>(KEY_FILTER) ?: TodoFilter.All,
            isLoading = restoredTodos == null,
        ),
    )
    val uiState: StateFlow<TodoListUiState> = _uiState.asStateFlow()

    init {
        if (restoredTodos == null) load()
    }

    // API から取得して、成功なら一覧を入れ、失敗なら失敗の状態にする。
    // viewModelScope: ViewModel が破棄される（onCleared）ときに、中の処理もまとめてキャンセルされるスコープ。
    // 既定のディスパッチャーは Dispatchers.Main.immediate。通信は Ktor のエンジンが別のスレッドで行うので、withContext は要らない
    private fun load() {
        viewModelScope.launch {
            try {
                val todos = api.fetchTodos()
                update { it.copy(todos = todos, isLoading = false, errorMessage = null) }
            } catch (e: CancellationException) {
                // キャンセルは「失敗」ではない。握りつぶさずに投げ直す（握りつぶすと、キャンセルされたコルーチンが止まらない）
                throw e
            } catch (e: Exception) {
                // 失敗の状態は SavedStateHandle に書かない。書くと、復元のときに「読み込み済みの空の一覧」と見分けがつかず、取得し直されない
                _uiState.update { it.copy(isLoading = false, errorMessage = e.toUserMessage()) }
            }
        }
    }

    // 失敗中の「再試行」。読み込み中に戻してから、もう一度取得する（読み込み中・失敗していないときは何もしない）
    fun retry() {
        if (_uiState.value.errorMessage == null) return
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        load()
    }

    // 状態の更新はすべてここを通す。updateAndGet は、今の値から新しい値を作って入れ替え、入れ替えたあとの値を返す
    // （複数の更新が重なっても取りこぼさない）。その値を SavedStateHandle にも書く
    private fun update(transform: (TodoListUiState) -> TodoListUiState) {
        val newState = _uiState.updateAndGet(transform)
        // 読み込み中・失敗中の一覧は「まだ取得していない空」なので保存しない（保存すると、復元のときに取得し直されない）
        if (!newState.isLoading && newState.errorMessage == null) {
            savedStateHandle[KEY_TODOS] = ArrayList(newState.todos)
            savedStateHandle[KEY_FILTER] = newState.filter
        }
    }

    fun addTodo(title: String) {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return
        update { state ->
            // id は今ある最大値 + 1
            val id = (state.todos.maxOfOrNull { it.id } ?: 0) + 1
            state.copy(todos = state.todos + Todo(id = id, title = trimmed))
        }
    }

    fun toggleTodo(id: Int, done: Boolean) {
        update { state ->
            state.copy(todos = state.todos.map { if (it.id == id) it.copy(done = done) else it })
        }
    }

    fun deleteTodo(id: Int) {
        update { state -> state.copy(todos = state.todos.filterNot { it.id == id }) }
    }

    // 削除の「元に戻す」。削除前の位置に戻す（その間に件数が減っていても範囲内に収める）
    fun restoreTodo(todo: Todo, index: Int) {
        update { state ->
            val todos = state.todos.toMutableList()
            todos.add(index.coerceIn(0, todos.size), todo)
            state.copy(todos = todos)
        }
    }

    fun deleteDone() {
        update { state -> state.copy(todos = state.todos.filterNot { it.done }) }
    }

    fun setFilter(filter: TodoFilter) {
        update { it.copy(filter = filter) }
    }

    // 例外を、画面に出す文言にする。e.message はそのまま出さない（レスポンスの本文が入ることがある）
    private fun Exception.toUserMessage(): String = when (this) {
        // タイムアウトは IOException のサブクラスもあるので、IOException より先に調べる
        is HttpRequestTimeoutException, is ConnectTimeoutException, is SocketTimeoutException -> "通信がタイムアウトしました"
        is ResponseException -> "サーバーがエラーを返しました（HTTP ${response.status.value}）"
        is IOException -> "ネットワークに接続できません"
        is ContentConvertException, is SerializationException, is NoTransformationFoundException -> "データの形式が正しくありません"
        else -> "予期しないエラーが発生しました"
    }

    private companion object {
        const val KEY_TODOS = "todos"
        const val KEY_FILTER = "filter"
    }
}
