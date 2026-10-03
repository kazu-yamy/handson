package com.handson.android01.ui.screens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.handson.android01.kotlin.Todo
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch

// 画面状態を持つ ViewModel。画面回転（Activity の作り直し）では破棄されず、同じインスタンスが使われ続ける。
// SavedStateHandle: プロセスが終了されても残したい値の入れ物。Activity の onSaveInstanceState の Bundle に保存され、
// プロセスが作り直されたときに、保存した値が入った状態で渡される
class TodoListViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val initialTodos: List<Todo>,
    // 読み込みに見立てて待つ時間。テストでは 0 や仮想時間で動かす
    private val loadDelayMillis: Long,
) : ViewModel() {

    // viewModel() の既定のファクトリは、引数なし、または SavedStateHandle だけのコンストラクタを探して呼ぶ
    constructor(savedStateHandle: SavedStateHandle) : this(savedStateHandle, demoTodos(20), loadDelayMillis = 1_000L)

    // 書き込めるのは ViewModel の中だけ。外には読み取り専用の StateFlow として公開する。
    // 保存されていればそれを使う（読み込み済みなので isLoading は false）。無ければ空の一覧と isLoading = true で始め、下の init で読み込む。
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
        if (restoredTodos == null) {
            // viewModelScope: ViewModel が破棄される（onCleared）ときに、中の処理もまとめてキャンセルされるスコープ。
            // 既定のディスパッチャーは Dispatchers.Main.immediate
            viewModelScope.launch {
                // 読み込みに見立てた待ち時間（実際のアプリでは、ここでサーバーや DB から読む）
                delay(loadDelayMillis)
                update { it.copy(todos = initialTodos, isLoading = false) }
            }
        }
    }

    // 状態の更新はすべてここを通す。updateAndGet は、今の値から新しい値を作って入れ替え、入れ替えたあとの値を返す
    // （複数の更新が重なっても取りこぼさない）。その値を SavedStateHandle にも書く
    private fun update(transform: (TodoListUiState) -> TodoListUiState) {
        val newState = _uiState.updateAndGet(transform)
        savedStateHandle[KEY_TODOS] = ArrayList(newState.todos)
        savedStateHandle[KEY_FILTER] = newState.filter
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

    private companion object {
        const val KEY_TODOS = "todos"
        const val KEY_FILTER = "filter"
    }
}
