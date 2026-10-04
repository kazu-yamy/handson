package com.handson.android01.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.handson.android01.data.TodoApi
import com.handson.android01.data.TodoDao
import com.handson.android01.ui.screens.FakeDataStore
import com.handson.android01.ui.screens.FakeTodoDao
import com.handson.android01.ui.screens.FakeTodoServer
import com.handson.android01.ui.screens.demoTodos
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

// androidTest のすべての @HiltAndroidTest で、DataModule の代わりにこの module が使われる。
// 本物の DB にも DataStore のファイルにもネットワークにも出ない。AppDatabase は提供しない（TodoDao を直接、偽物にするので作られない）。
// HiltTestApplication はテストごとにコンポーネントを作り直すので、@Singleton の偽物もテストごとに新しくなる
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [DataModule::class])
object FakeDataModule {

    @Provides
    @Singleton
    fun provideTodoDao(): TodoDao = FakeTodoDao()

    @Provides
    @Singleton
    fun provideTodoApi(): TodoApi = FakeTodoServer(demoTodos(20)).api

    @Provides
    @Singleton
    fun provideSettingsDataStore(): DataStore<Preferences> = FakeDataStore()
}
