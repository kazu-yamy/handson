package com.handson.android01.ui.screens

import androidx.lifecycle.SavedStateHandle
import com.handson.android01.kotlin.Todo
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// ViewModel の単体テスト（JVM 上。エミュレータは不要）。
// viewModelScope は Dispatchers.Main.immediate を使うが、JVM には Main が無いので、setMain でテスト用のディスパッチャーに差し替える
@OptIn(ExperimentalCoroutinesApi::class)
class TodoListViewModelTest {

    private val todos = listOf(
        Todo(1, "write"),
        Todo(2, "test", done = true),
        Todo(3, "ship"),
    )

    // setMain したら、テストのあとで必ず resetMain する（他のテストに差し替えが残らないように）
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // 偽のサーバーは、渡されたディスパッチャーで動く。テスト用のディスパッチャーなら、応答までの待ち時間（delayMillis）は仮想時間になる
    private fun viewModel(
        saved: SavedStateHandle = SavedStateHandle(),
        responseDelayMillis: Long = 1_000L,
        dispatcher: CoroutineDispatcher,
    ) = TodoListViewModel(saved, FakeTodoServer(todos, delayMillis = responseDelayMillis, dispatcher = dispatcher).api)

    // StandardTestDispatcher: launch した処理はキューに入るだけで、テスト側が進める（runCurrent / advanceTimeBy / advanceUntilIdle）まで動かない。
    // delay は仮想時間なので、待たずに時間だけ進められる
    @Test
    fun standardDispatcher_loadingFinishesWhenVirtualTimeAdvances() = runTest(StandardTestDispatcher()) {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val vm = viewModel(responseDelayMillis = 1_000L, dispatcher = dispatcher)

        // 作った直後: 読み込み中で、一覧は空
        assertTrue(vm.uiState.value.isLoading)
        assertEquals(emptyList<Todo>(), vm.uiState.value.todos)

        advanceTimeBy(999)
        runCurrent()
        assertTrue(vm.uiState.value.isLoading)

        advanceTimeBy(1)
        runCurrent()
        assertFalse(vm.uiState.value.isLoading)
        assertEquals(todos, vm.uiState.value.todos)
    }

    // 待ち時間が 0 でも、Standard では launch の中身は runCurrent するまで動かない
    @Test
    fun standardDispatcher_doesNotRunLaunchedWorkUntilTold() = runTest(StandardTestDispatcher()) {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val vm = viewModel(responseDelayMillis = 0L, dispatcher = dispatcher)

        assertTrue(vm.uiState.value.isLoading)

        runCurrent()
        assertFalse(vm.uiState.value.isLoading)
    }

    // UnconfinedTestDispatcher: launch した処理は、最初の中断点（delay など）まで、呼び出した場所でそのまま実行される
    @Test
    fun unconfinedDispatcher_runsLaunchedWorkEagerly() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val vm = viewModel(responseDelayMillis = 0L, dispatcher = dispatcher)

        // runCurrent なしで、もう読み込みが終わっている
        assertFalse(vm.uiState.value.isLoading)
        assertEquals(todos, vm.uiState.value.todos)
    }

    // StateFlow の値の変化を順に集める。collect は終わらないので backgroundScope で起動する（テストの最後に自動でキャンセルされる）。
    // StateFlow は値が変わったときだけ通知し、collect が追いつく前の途中の値は飛ばされる（conflation）
    @Test
    fun uiState_emitsLoadingThenLoadedThenToggled() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val vm = viewModel(responseDelayMillis = 1_000L, dispatcher = dispatcher)
        val states = mutableListOf<TodoListUiState>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.toList(states) }

        advanceTimeBy(1_000)
        runCurrent()
        vm.toggleTodo(1, done = true)

        assertEquals(3, states.size)
        assertTrue(states[0].isLoading)
        assertFalse(states[1].isLoading)
        assertFalse(states[1].todos.first { it.id == 1 }.done)
        assertTrue(states[2].todos.first { it.id == 1 }.done)
    }

    // 以降のテストは、読み込み済み（待ち時間 0 + Unconfined）の状態から始める
    private fun loadedViewModel(saved: SavedStateHandle = SavedStateHandle()): TodoListViewModel {
        val dispatcher = UnconfinedTestDispatcher()
        Dispatchers.setMain(dispatcher)
        return viewModel(saved, responseDelayMillis = 0L, dispatcher = dispatcher)
    }

    @Test
    fun toggleTodo_changesOnlyThatTodo() {
        val vm = loadedViewModel()

        vm.toggleTodo(1, done = true)

        assertEquals(listOf(true, true, false), vm.uiState.value.todos.map { it.done })
    }

    @Test
    fun addTodo_appendsWithNextIdAndIgnoresBlank() {
        val vm = loadedViewModel()

        vm.addTodo("  buy milk  ")
        vm.addTodo("   ")

        assertEquals(4, vm.uiState.value.todos.size)
        assertEquals(Todo(4, "buy milk"), vm.uiState.value.todos.last())
    }

    @Test
    fun deleteTodo_thenRestoreTodo_putsItBackAtTheSameIndex() {
        val vm = loadedViewModel()
        val target = todos[1]

        vm.deleteTodo(target.id)
        assertEquals(listOf(1, 3), vm.uiState.value.todos.map { it.id })

        vm.restoreTodo(target, index = 1)
        assertEquals(listOf(1, 2, 3), vm.uiState.value.todos.map { it.id })
    }

    @Test
    fun deleteDone_removesDoneTodos() {
        val vm = loadedViewModel()

        vm.deleteDone()

        assertEquals(listOf(1, 3), vm.uiState.value.todos.map { it.id })
    }

    @Test
    fun setFilter_changesFilterOnly() {
        val vm = loadedViewModel()

        vm.setFilter(TodoFilter.Done)

        assertEquals(TodoFilter.Done, vm.uiState.value.filter)
        assertEquals(todos, vm.uiState.value.todos)
    }

    // SavedStateHandle に保存された値があれば、それを使い、読み込みは行わない（プロセスの再生成を、保存済みのハンドルを渡して再現する）
    @Test
    fun restoredState_isUsedWithoutLoading() {
        val saved = SavedStateHandle(
            mapOf(
                "todos" to arrayListOf(Todo(9, "restored", done = true)),
                "filter" to TodoFilter.Done,
            ),
        )

        val vm = loadedViewModel(saved)

        assertFalse(vm.uiState.value.isLoading)
        assertEquals(listOf(Todo(9, "restored", done = true)), vm.uiState.value.todos)
        assertEquals(TodoFilter.Done, vm.uiState.value.filter)
    }

    // 更新のたびに SavedStateHandle にも書かれる（保存される値は、Activity が onSaveInstanceState で Bundle に入れる）
    @Test
    fun updates_areWrittenToSavedStateHandle() {
        val saved = SavedStateHandle()
        val vm = loadedViewModel(saved)

        vm.toggleTodo(1, done = true)
        vm.setFilter(TodoFilter.Active)

        assertEquals(vm.uiState.value.todos, saved.get<ArrayList<Todo>>("todos"))
        assertEquals(TodoFilter.Active, saved.get<TodoFilter>("filter"))
    }
}
