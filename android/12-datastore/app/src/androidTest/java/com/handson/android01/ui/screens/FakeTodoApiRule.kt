package com.handson.android01.ui.screens

import com.handson.android01.data.AppContainer
import com.handson.android01.data.SettingsRepository
import com.handson.android01.data.TodoRepository
import org.junit.rules.TestWatcher
import org.junit.runner.Description

// 実際の Activity（MainActivity）を起動するテスト用: アプリが使う TodoRepository と SettingsRepository を、本物の DB にも DataStore のファイルにもネットワークにも出ない（偽の DAO・偽のサーバー・偽の DataStore の）ものに差し替える。
// Activity を起動するルールより外側に置く（RuleChain.outerRule）。そうしないと、起動のあとに差し替えることになる
class FakeTodoApiRule(val server: FakeTodoServer = FakeTodoServer(demoTodos(20))) : TestWatcher() {

    override fun starting(description: Description) {
        val settings = SettingsRepository(FakeDataStore())
        AppContainer.settingsRepositoryOverride = settings
        AppContainer.todoRepositoryOverride = TodoRepository(FakeTodoDao(), server.api, settings)
    }

    override fun finished(description: Description) {
        AppContainer.todoRepositoryOverride = null
        AppContainer.settingsRepositoryOverride = null
    }
}
