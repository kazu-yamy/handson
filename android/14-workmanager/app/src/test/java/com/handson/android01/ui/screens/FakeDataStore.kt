package com.handson.android01.ui.screens

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

// テスト用の偽の DataStore。ファイルには書かず、メモリ上の Preferences を MutableStateFlow に入れて動かす。
// SettingsRepository が使うキーの変換は本物のまま動くので、保存と復元を（ファイルなしで）確かめられる。
// initial は「前回の起動で保存した」状態を作るためのもの
class FakeDataStore(initial: Preferences = emptyPreferences()) : DataStore<Preferences> {

    private val state = MutableStateFlow(initial)

    override val data: Flow<Preferences> = state

    override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
        val updated = transform(state.value)
        state.update { updated }
        return updated
    }
}
