package com.handson.android01.data

import androidx.room3.Database
import androidx.room3.RoomDatabase

// DB 本体の定義。テーブルになる Entity の一覧と、スキーマの版（version）を書く。
// Entity を変えたら version を上げて、移行（Migration）を用意する
@Database(entities = [TodoEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun todoDao(): TodoDao
}
