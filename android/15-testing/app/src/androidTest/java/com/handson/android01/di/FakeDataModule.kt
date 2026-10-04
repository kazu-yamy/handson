package com.handson.android01.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.handson.android01.data.TodoDao
import com.handson.android01.ui.screens.FakeDataStore
import com.handson.android01.ui.screens.FakeTodoDao
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

// androidTest のすべての @HiltAndroidTest で、DataModule の代わりにこの module が使われる。
// 本物の DB にも DataStore のファイルにも出ない。AppDatabase は提供しない（TodoDao を直接、偽物にするので作られない）。
// API の偽物は FakeApiModule（こちらは @InstallIn なので、テストクラスが @UninstallModules で外せる）が提供する。
// HiltTestApplication はテストごとにコンポーネントを作り直すので、@Singleton の偽物もテストごとに新しくなる
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [DataModule::class])
object FakeDataModule {

    @Provides
    @Singleton
    fun provideTodoDao(): TodoDao = FakeTodoDao()

    @Provides
    @Singleton
    fun provideSettingsDataStore(): DataStore<Preferences> = FakeDataStore()
}
