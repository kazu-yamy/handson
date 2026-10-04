package com.handson.android01.di

import com.handson.android01.data.TodoApi
import com.handson.android01.ui.screens.FakeTodoServer
import com.handson.android01.ui.screens.demoTodos
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// 偽の API。FakeDataModule（@TestInstallIn）とは分けて、@InstallIn にする。
// @UninstallModules で外せるのは @InstallIn の module だけ（@TestInstallIn の module は外せない）。
// こうしておくと、ほとんどのテストはこの偽物を使い、API を差し替えたいテストクラスだけが、これを外して自分の偽物を入れられる
@Module
@InstallIn(SingletonComponent::class)
object FakeApiModule {

    @Provides
    @Singleton
    fun provideTodoApi(): TodoApi = FakeTodoServer(demoTodos(20)).api
}
