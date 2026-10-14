package com.rainweaver.moneymgr.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Insert
    suspend fun insert(tx: Transaction): Long

    @Update
    suspend fun update(tx: Transaction)

    @Delete
    suspend fun delete(tx: Transaction)

    @Query("SELECT * FROM transactions ORDER BY dateEpochDay DESC, id DESC")
    fun observeAll(): Flow<List<Transaction>>

    // date-range aware queries (used by history / summary / reports)
    @Query("SELECT * FROM transactions WHERE dateEpochDay BETWEEN :from AND :to ORDER BY dateEpochDay DESC, id DESC")
    fun observeRange(from: Long, to: Long): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE dateEpochDay BETWEEN :from AND :to ORDER BY dateEpochDay ASC, id ASC")
    suspend fun inRange(from: Long, to: Long): List<Transaction>

    @Query("SELECT COALESCE(SUM(CASE WHEN type = 'INCOME' THEN amountMinor ELSE 0 END),0) FROM transactions WHERE dateEpochDay BETWEEN :from AND :to")
    suspend fun incomeBetween(from: Long, to: Long): Long

    @Query("SELECT COALESCE(SUM(CASE WHEN type = 'EXPENSE' THEN amountMinor ELSE 0 END),0) FROM transactions WHERE dateEpochDay BETWEEN :from AND :to")
    suspend fun expenseBetween(from: Long, to: Long): Long

    @Query("SELECT COALESCE(SUM(amountMinor),0) FROM transactions WHERE categoryId = :categoryId AND type = 'EXPENSE' AND dateEpochDay BETWEEN :from AND :to")
    suspend fun expenseByCategory(categoryId: Long, from: Long, to: Long): Long
}
