package com.rainweaver.moneymgr.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Insert
    suspend fun insert(c: Category): Long

    @Insert
    suspend fun insertAll(cs: List<Category>)

    @Update
    suspend fun update(c: Category)

    @Delete
    suspend fun delete(c: Category)

    @Query("SELECT * FROM categories ORDER BY type, name")
    fun observeAll(): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE type = :type ORDER BY name")
    suspend fun byType(type: TxType): List<Category>

    @Query("SELECT COUNT(*) FROM transactions WHERE categoryId = :id")
    suspend fun usageCount(id: Long): Int

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun count(): Int
}
