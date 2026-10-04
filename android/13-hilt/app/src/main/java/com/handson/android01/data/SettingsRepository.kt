package com.handson.android01.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// アプリの設定の入口。保存先は Preferences DataStore（キーと値の組を、1 つのファイルに保存する）。
// DataStore<Preferences> は引数で受け取る（テストでは、メモリ上の偽物を渡すため）
@Singleton
class SettingsRepository @Inject constructor(private val dataStore: DataStore<Preferences>) {

    // 「初回の取得が済んだか」。キーは型ごとの関数（booleanPreferencesKey）で作り、名前は保存されるキーそのもの
    suspend fun isInitialFetchDone(): Boolean = dataStore.data.first()[INITIAL_FETCH_DONE] ?: false

    // edit は、読み取り → 変更 → 書き込みを 1 つのトランザクションとして行う。保存が終わるまで中断する
    suspend fun markInitialFetchDone() {
        dataStore.edit { it[INITIAL_FETCH_DONE] = true }
    }

    // 一覧の絞り込み。enum の name（"Active" など）を文字列で保存する。
    // 保存が無い（初回）・知らない名前（将来の版で消した値など）のときは、すべて
    val filter: Flow<TodoFilter> = dataStore.data.map { prefs ->
        TodoFilter.entries.firstOrNull { it.name == prefs[FILTER] } ?: TodoFilter.All
    }

    suspend fun setFilter(filter: TodoFilter) {
        dataStore.edit { it[FILTER] = filter.name }
    }

    private companion object {
        val FILTER = stringPreferencesKey("filter")
        val INITIAL_FETCH_DONE = booleanPreferencesKey("initial_fetch_done")
    }
}
