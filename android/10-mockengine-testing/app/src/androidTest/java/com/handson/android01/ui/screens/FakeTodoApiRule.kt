package com.handson.android01.ui.screens

import com.handson.android01.data.AppContainer
import org.junit.rules.TestWatcher
import org.junit.runner.Description

// 実際の Activity（MainActivity）を起動するテスト用: アプリが使う TodoApi を、ネットワークに出ない偽のサーバーのものに差し替える。
// Activity を起動するルールより外側に置く（RuleChain.outerRule）。そうしないと、起動のあとに差し替えることになる
class FakeTodoApiRule(val server: FakeTodoServer = FakeTodoServer(demoTodos(20))) : TestWatcher() {

    override fun starting(description: Description) {
        AppContainer.todoApiOverride = server.api
    }

    override fun finished(description: Description) {
        AppContainer.todoApiOverride = null
    }
}
