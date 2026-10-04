package com.handson.android01.data

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow

// DB への操作の一覧。interface を書くと、実装（TodoDao_Impl）を Room が KSP で生成する。
// Room 3 では、DAO の関数は suspend にする。変更を監視する関数だけは、Flow を返す（suspend は付けない）
@Dao
interface TodoDao {

    // テーブルが変わるたびに、最新の一覧を流し直す
    @Query("SELECT * FROM todos ORDER BY id")
    fun observeAll(): Flow<List<TodoEntity>>

    @Query("SELECT COUNT(*) FROM todos")
    suspend fun count(): Int

    // 同じ id の行があれば置き換える
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(todo: TodoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(todos: List<TodoEntity>)

    @Query("UPDATE todos SET done = :done WHERE id = :id")
    suspend fun setDone(id: Int, done: Boolean)

    @Query("DELETE FROM todos WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM todos WHERE done = 1")
    suspend fun deleteDone()
}
