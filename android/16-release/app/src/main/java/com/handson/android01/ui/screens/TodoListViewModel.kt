package com.handson.android01.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.handson.android01.data.SettingsRepository
import com.handson.android01.data.TodoFilter
import com.handson.android01.data.TodoRepository
import com.handson.android01.kotlin.Todo
import dagger.hilt.android.lifecycle.HiltViewModel
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
import javax.inject.Inject

// 画面状態を持つ ViewModel。画面回転（Activity の作り直し）では破棄されず、同じインスタンスが使われ続ける。
// @HiltViewModel + @Inject constructor: Hilt が、引数の TodoRepository と SettingsRepository を組み立てて渡す
@HiltViewModel
class TodoListViewModel @Inject constructor(
    // Todo の保存先と取得元。Hilt が DataModule と @Inject constructor から組み立てて渡す。単体テストでは自分で渡す（偽の DAO と、通信しない MockEngine 入りの TodoApi をつないだもの）
    private val repository: TodoRepository,
    // 絞り込みの保存先（DataStore）。アプリを終了しても残る。テストでは、メモリ上の偽の DataStore をつないだものを渡す
    private val settings: SettingsRepository,
) : ViewModel() {

    // 読み込み中かどうかと失敗の文言。DB には入らない画面だけの状態なので、ViewModel のメモリだけに持つ（復元しない）
    private data class LoadState(val isLoading: Boolean = true, val errorMessage: String? = null)

    private val loadState = MutableStateFlow(LoadState())

    // 画面の状態は、4 つの Flow を combine して作る: DB の一覧（Room）・絞り込み（DataStore）・読み込みの状態・最終バックアップの時刻（DataStore）。
    // combine は、4 つがすべて 1 回は値を出すまで何も流さない。DataStore の読み込みは非同期だが、保存した絞り込みが届くまでは
    // 初期値（読み込み中）のままなので、既定の「すべて」が一瞬見えることはない
    // どれかが変わるたびに作り直されるので、一覧を書き換える処理は ViewModel に要らない（DAO に書けば、Flow で届く）。
    // stateIn: 冷たい Flow を、現在の値を持つ StateFlow にする。Eagerly は、ViewModel が作られた時点から購読を始める
    val uiState: StateFlow<TodoListUiState> = combine(
        repository.todos,
        settings.filter,
        loadState,
        settings.lastBackupAt,
    ) { todos, filter, load, lastBackupAt ->
        TodoListUiState(
            todos = todos,
            filter = filter,
            isLoading = load.isLoading,
            errorMessage = load.errorMessage,
            lastBackupAt = lastBackupAt,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, TodoListUiState(isLoading = true))

    init {
        load()
    }

    // 「取得済み」の印が無いときだけ API から取得して保存し、成功なら読み込み済みにして、失敗なら失敗の状態にする。
    // viewModelScope: ViewModel が破棄される（onCleared）ときに、中の処理もまとめてキャンセルされるスコープ。
    // 既定のディスパッチャーは Dispatchers.Main.immediate。DB の処理は Room が（DataModule で指定した）IO のスレッドで、
    // 通信は Ktor のエンジンが別のスレッドで行うので、withContext は要らない
    private fun load() {
        viewModelScope.launch {
            try {
                val saved = repository.fetchInitialTodosIfNeeded()
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

    // 絞り込みは DataStore に書く。書き込みが終わると、上の settings.filter を通って画面に届く
    fun setFilter(filter: TodoFilter) {
        viewModelScope.launch { settings.setFilter(filter) }
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
}
