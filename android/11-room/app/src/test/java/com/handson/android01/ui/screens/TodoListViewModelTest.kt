package com.handson.android01.ui.screens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.handson.android01.data.TodoRepository
import com.handson.android01.kotlin.Todo
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

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
        dao: FakeTodoDao = FakeTodoDao(),
        responseDelayMillis: Long = 1_000L,
        dispatcher: CoroutineDispatcher,
    ) = TodoListViewModel(saved, TodoRepository(dao, FakeTodoServer(todos, delayMillis = responseDelayMillis, dispatcher = dispatcher).api))

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
    // StateFlow は値が変わったときだけ通知し、collect が追いつく前の途中の値は飛ばされる（conflation）。
    // 一覧が届いてから読み込み済みにするので、「一覧は入ったが、まだ読み込み中」の状態が途中に 1 つ入る（ViewModel の load を参照）
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
        runCurrent()

        assertEquals(4, states.size)
        assertTrue(states[0].isLoading)
        assertEquals(emptyList<Todo>(), states[0].todos)
        assertTrue(states[1].isLoading)
        assertEquals(todos, states[1].todos)
        assertFalse(states[2].isLoading)
        assertFalse(states[2].todos.first { it.id == 1 }.done)
        assertTrue(states[3].todos.first { it.id == 1 }.done)
    }

    // 以降のテストは、読み込み済み（待ち時間 0 + Unconfined）の状態から始める
    private fun loadedViewModel(
        saved: SavedStateHandle = SavedStateHandle(),
        dao: FakeTodoDao = FakeTodoDao(),
    ): TodoListViewModel {
        val dispatcher = UnconfinedTestDispatcher()
        Dispatchers.setMain(dispatcher)
        return viewModel(saved, dao, responseDelayMillis = 0L, dispatcher = dispatcher)
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

        vm.restoreTodo(target)
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

    // DB にすでに一覧があれば、それをそのまま表示し、読み込み中にならない（アプリの再起動を、中身のある DAO を渡して再現する）
    @Test
    fun dbHasTodos_isShownWithoutLoading() {
        val saved = SavedStateHandle(mapOf("filter" to TodoFilter.Done))

        val vm = loadedViewModel(saved, FakeTodoDao(listOf(Todo(9, "saved", done = true))))

        assertFalse(vm.uiState.value.isLoading)
        assertEquals(listOf(Todo(9, "saved", done = true)), vm.uiState.value.todos)
        assertEquals(TodoFilter.Done, vm.uiState.value.filter)
    }

    // 一覧の変更は DAO に書かれ、SavedStateHandle に書かれるのは絞り込みだけ
    @Test
    fun updates_areWrittenToDao() = runTest {
        val saved = SavedStateHandle()
        val dao = FakeTodoDao()
        val vm = loadedViewModel(saved, dao)

        vm.toggleTodo(1, done = true)
        vm.setFilter(TodoFilter.Active)

        assertEquals(3, dao.count())
        assertEquals(vm.uiState.value.todos, dao.observeAll().first().map { Todo(it.id, it.title, it.done) })
        assertTrue(vm.uiState.value.todos.first { it.id == 1 }.done)
        assertEquals(TodoFilter.Active, saved.get<TodoFilter>("filter"))
        assertFalse(saved.contains("todos"))
    }

    // ---- 読み込みの失敗と再試行（偽のサーバーが失敗を返す） ----

    // 偽のサーバーと、それにつないだ ViewModel を作る。ディスパッチャーは Unconfined なので、応答は呼んだ場所でそのまま処理される
    private fun failingViewModel(
        saved: SavedStateHandle = SavedStateHandle(),
        dao: FakeTodoDao = FakeTodoDao(),
        configure: FakeTodoServer.() -> Unit,
    ): Pair<FakeTodoServer, TodoListViewModel> {
        val dispatcher = UnconfinedTestDispatcher()
        Dispatchers.setMain(dispatcher)
        val server = FakeTodoServer(todos, dispatcher = dispatcher).apply(configure)
        return server to TodoListViewModel(saved, TodoRepository(dao, server.api))
    }

    @Test
    fun loadFailure_showsErrorAndStopsLoading() {
        val (_, vm) = failingViewModel { status = HttpStatusCode.NotFound }

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertEquals("サーバーがエラーを返しました（HTTP 404）", state.errorMessage)
        assertEquals(emptyList<Todo>(), state.todos)
    }

    @Test
    fun loadFailure_messageDependsOnTheKindOfException() {
        val (_, network) = failingViewModel { error = IOException("boom") }
        assertEquals("ネットワークに接続できません", network.uiState.value.errorMessage)

        val (_, broken) = failingViewModel { rawBody = "[{" }
        assertEquals("データの形式が正しくありません", broken.uiState.value.errorMessage)

        val (_, unexpected) = failingViewModel { error = IllegalStateException("bug") }
        assertEquals("予期しないエラーが発生しました", unexpected.uiState.value.errorMessage)
    }

    // 例外の message（レスポンスの本文が入ることがある）は、画面に出す文言に混ぜない
    @Test
    fun loadFailure_doesNotLeakExceptionMessage() {
        val (_, vm) = failingViewModel { rawBody = "[{\"secret\": 1}]" }

        assertEquals("データの形式が正しくありません", vm.uiState.value.errorMessage)
    }

    // 失敗したときは、DB に何も書かない（空のままなので、次の起動でも取得し直される）
    @Test
    fun loadFailure_leavesDaoEmpty() = runTest {
        val dao = FakeTodoDao()

        failingViewModel(dao = dao) { status = HttpStatusCode.InternalServerError }

        assertEquals(0, dao.count())
    }

    @Test
    fun retry_afterFailure_loadsTodos() {
        val (server, vm) = failingViewModel { status = HttpStatusCode.InternalServerError }
        assertEquals("サーバーがエラーを返しました（HTTP 500）", vm.uiState.value.errorMessage)

        server.status = HttpStatusCode.OK
        vm.retry()

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertEquals(null, state.errorMessage)
        assertEquals(todos, state.todos)
        assertEquals(2, server.requestCount)
    }

    // 再試行は、失敗中にだけ効く。読み込み済みのときに呼んでも、取得し直さない
    @Test
    fun retry_whenNotFailed_doesNothing() {
        val (server, vm) = failingViewModel { }

        vm.retry()

        assertEquals(todos, vm.uiState.value.todos)
        assertEquals(1, server.requestCount)
    }

    // 再試行の途中（読み込み中）に、もう一度押されても、取得は 1 回だけ
    @Test
    fun retry_whileLoading_doesNotStartSecondRequest() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val server = FakeTodoServer(todos, status = HttpStatusCode.InternalServerError, delayMillis = 1_000L, dispatcher = dispatcher)
        val vm = TodoListViewModel(SavedStateHandle(), TodoRepository(FakeTodoDao(), server.api))
        advanceTimeBy(1_000)
        runCurrent()
        assertEquals("サーバーがエラーを返しました（HTTP 500）", vm.uiState.value.errorMessage)

        server.status = HttpStatusCode.OK
        vm.retry()
        vm.retry()
        // loadState の変更が uiState に届くのは、combine がテスト用のディスパッチャーで動いてから
        runCurrent()
        assertTrue(vm.uiState.value.isLoading)
        advanceTimeBy(1_000)
        runCurrent()

        assertEquals(todos, vm.uiState.value.todos)
        assertEquals(2, server.requestCount)
    }

    // 取得の途中で ViewModel が破棄されると、viewModelScope がキャンセルされ、取得中のコルーチンも止まる。
    // これは失敗ではないので、失敗の表示（errorMessage）にしてはいけない（catch (e: Exception) で握りつぶさず、CancellationException は投げ直す）
    @Test
    fun cancellationDuringLoad_isNotTreatedAsFailure() = runTest(StandardTestDispatcher()) {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val vm = viewModel(responseDelayMillis = 1_000L, dispatcher = dispatcher)

        // 応答を待っている途中（1 秒のうち 0.5 秒）で、ViewModel の破棄と同じく viewModelScope をキャンセルする
        advanceTimeBy(500)
        runCurrent()
        vm.viewModelScope.cancel()
        advanceUntilIdle()

        assertEquals(null, vm.uiState.value.errorMessage)
    }

    // DB に一覧があれば取得しない（アプリの再起動）。通信の回数で確かめる
    @Test
    fun dbHasTodos_doesNotCallApi() {
        val (server, vm) = failingViewModel(dao = FakeTodoDao(listOf(Todo(9, "saved")))) { }

        assertEquals(0, server.requestCount)
        assertEquals(listOf(Todo(9, "saved")), vm.uiState.value.todos)
    }
}
