package com.handson.android01.ui.screens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.handson.android01.data.AppContainer
import com.handson.android01.data.TodoRepository
import com.handson.android01.kotlin.Todo
import io.ktor.client.call.NoTransformationFoundException
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ResponseException
import io.ktor.serialization.ContentConvertException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import java.io.IOException

// 画面状態を持つ ViewModel。画面回転（Activity の作り直し）では破棄されず、同じインスタンスが使われ続ける。
// SavedStateHandle: プロセスが終了されても残したい値の入れ物。Activity の onSaveInstanceState の Bundle に保存され、
// プロセスが作り直されたときに、保存した値が入った状態で渡される
class TodoListViewModel(
    private val savedStateHandle: SavedStateHandle,
    // Todo の保存先と取得元。テストでは、偽の DAO と、通信しない MockEngine 入りの TodoApi をつないだものを渡す
    private val repository: TodoRepository,
) : ViewModel() {

    // viewModel() の既定のファクトリは、引数なし、または SavedStateHandle だけのコンストラクタを探して呼ぶ。
    // そのため、アプリでは TodoRepository を AppContainer（手書きの置き場所）から受け取る
    constructor(savedStateHandle: SavedStateHandle) : this(savedStateHandle, AppContainer.todoRepository)

    // 読み込み中かどうかと失敗の文言。DB には入らない画面だけの状態なので、ViewModel のメモリだけに持つ（復元しない）
    private data class LoadState(val isLoading: Boolean = true, val errorMessage: String? = null)

    private val loadState = MutableStateFlow(LoadState())

    // 画面の状態は、3 つの Flow を combine して作る: DB の一覧（Room）・絞り込み（SavedStateHandle）・読み込みの状態。
    // どれかが変わるたびに作り直されるので、一覧を書き換える処理は ViewModel に要らない（DAO に書けば、Flow で届く）。
    // stateIn: 冷たい Flow を、現在の値を持つ StateFlow にする。Eagerly は、ViewModel が作られた時点から購読を始める
    val uiState: StateFlow<TodoListUiState> = combine(
        repository.todos,
        savedStateHandle.getStateFlow(KEY_FILTER, TodoFilter.All),
        loadState,
    ) { todos, filter, load ->
        TodoListUiState(todos = todos, filter = filter, isLoading = load.isLoading, errorMessage = load.errorMessage)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, TodoListUiState(isLoading = true))

    init {
        load()
    }

    // DB が空のときだけ API から取得して保存し、成功なら読み込み済みにして、失敗なら失敗の状態にする。
    // viewModelScope: ViewModel が破棄される（onCleared）ときに、中の処理もまとめてキャンセルされるスコープ。
    // 既定のディスパッチャーは Dispatchers.Main.immediate。DB の処理は Room が（AppContainer で指定した）IO のスレッドで、
    // 通信は Ktor のエンジンが別のスレッドで行うので、withContext は要らない
    private fun load() {
        viewModelScope.launch {
            try {
                val saved = repository.refreshIfEmpty()
                // DB に書いてから、Room が新しい一覧を Flow に流すまでには少し間がある（実測で約 50 ms）。
                // その間に読み込み済みにすると、空の一覧が一瞬見えるので、一覧が届くまで読み込み中のままにする
                if (saved > 0) uiState.first { it.todos.isNotEmpty() }
                loadState.value = LoadState(isLoading = false)
            } catch (e: CancellationException) {
                // キャンセルは「失敗」ではない。握りつぶさずに投げ直す（握りつぶすと、キャンセルされたコルーチンが止まらない）
                throw e
            } catch (e: Exception) {
                loadState.value = LoadState(isLoading = false, errorMessage = e.toUserMessage())
            }
        }
    }

    // 失敗中の「再試行」。読み込み中に戻してから、もう一度取得する（読み込み中・失敗していないときは何もしない）
    fun retry() {
        if (loadState.value.errorMessage == null) return
        loadState.value = LoadState()
        load()
    }

    // 追加・切り替え・削除は、DAO に書くだけ。画面の一覧は、DB の変更が Flow で届いて更新される
    fun addTodo(title: String) {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch { repository.add(trimmed) }
    }

    fun toggleTodo(id: Int, done: Boolean) {
        viewModelScope.launch { repository.setDone(id, done) }
    }

    fun deleteTodo(id: Int) {
        viewModelScope.launch { repository.delete(id) }
    }

    // 削除の「元に戻す」。同じ id で入れ直すと、一覧の並び（id の順）で、削除前の位置に戻る
    fun restoreTodo(todo: Todo) {
        viewModelScope.launch { repository.restore(todo) }
    }

    fun deleteDone() {
        viewModelScope.launch { repository.deleteDone() }
    }

    // 絞り込みだけは SavedStateHandle に残す（プロセスが終了されても残る）。書き込むと、上の getStateFlow を通って画面に届く
    fun setFilter(filter: TodoFilter) {
        savedStateHandle[KEY_FILTER] = filter
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
        const val KEY_FILTER = "filter"
    }
}
