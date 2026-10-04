package com.handson.android01.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import com.handson.android01.data.AppDatabase
import com.handson.android01.data.TodoApi
import com.handson.android01.data.TodoDao
import com.handson.android01.data.createTodoHttpClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.ktor.client.engine.android.Android
import kotlinx.coroutines.Dispatchers
import javax.inject.Singleton

// DataStore は、ファイル（datastore/settings.preferences_pb）ごとに、アプリで 1 つだけ作る。
// preferencesDataStore のプロパティ委譲は、トップレベルに 1 回だけ書く（クラスの中に書くと、インスタンスのたびに別の DataStore が作られる）。
private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

// 自分で `@Inject constructor` を書けない、または組み立てが要る型（Room・Ktor・DataStore は他人の型。TodoApi は HttpClient を組み立てて渡す）の作り方を Hilt に教える module。
// SingletonComponent に入れるので、ここで作る部品はアプリ全体で使える
@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    // @Singleton は「コンポーネントに 1 つ」＝アプリで 1 つ。DB は何度も作らず、使い回す。
    // Context は Hilt が用意している（@ApplicationContext を付けると Application の Context が渡される）
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder<AppDatabase>(context, "todos.db")
            // クエリを実行するスレッド。ここでは IO 用のスレッドプールを指定する
            .setQueryCoroutineContext(Dispatchers.IO)
            // SQLite を動かすドライバー。省略しても既定でこのドライバーが使われるが、何を使っているか分かるように明示する
            .setDriver(AndroidSQLiteDriver())
            .build()

    // DAO は DB が持っているので、scope は付けない（呼ばれるたびに database.todoDao() を返すだけ）。
    // 引数の AppDatabase は、上の provideDatabase の結果を Hilt が渡す
    @Provides
    fun provideTodoDao(database: AppDatabase): TodoDao = database.todoDao()

    // HttpClient は接続を使い回すので、アプリで 1 つにする
    @Provides
    @Singleton
    fun provideTodoApi(): TodoApi = TodoApi(createTodoHttpClient(Android.create()))

    // 12 のトップレベルの委譲を返すだけ。「ファイルごとに 1 つ」の保証は、委譲のほうが持っている
    @Provides
    @Singleton
    fun provideSettingsDataStore(@ApplicationContext context: Context): DataStore<Preferences> = context.settingsDataStore
}
