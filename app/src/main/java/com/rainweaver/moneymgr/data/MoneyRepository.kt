package com.rainweaver.moneymgr.data

import android.content.Context
import kotlinx.coroutines.flow.Flow

class MoneyRepository(context: Context) {
    private val db = AppDatabase.get(context)
    private val txDao = db.transactionDao()
    private val catDao = db.categoryDao()

    // transactions
    fun observeTransactions(): Flow<List<Transaction>> = txDao.observeAll()
    fun observeRange(from: Long, to: Long): Flow<List<Transaction>> = txDao.observeRange(from, to)
    suspend fun addTx(tx: Transaction): Long = txDao.insert(tx)
    suspend fun updateTx(tx: Transaction) = txDao.update(tx)
    suspend fun deleteTx(tx: Transaction) = txDao.delete(tx)
    suspend fun allTx(): List<Transaction> = txDao.inRange(Long.MIN_VALUE, Long.MAX_VALUE)
    suspend fun txInRange(from: Long, to: Long): List<Transaction> = txDao.inRange(from, to)

    // categories
    fun observeCategories(): Flow<List<Category>> = catDao.observeAll()
    suspend fun categoriesByType(type: TxType): List<Category> = catDao.byType(type)
    suspend fun addCategory(c: Category): Long = catDao.insert(c)
    suspend fun updateCategory(c: Category) = catDao.update(c)
    suspend fun deleteCategory(c: Category) = catDao.delete(c)
    suspend fun categoryUsage(id: Long): Int = catDao.usageCount(id)
    suspend fun categoryCount(): Int = catDao.count()

    /** Seeds the predefined categories the first time the DB is used. */
    suspend fun seedDefaultsIfEmpty() {
        if (categoryCount() > 0) return
        val income = listOf("Salary", "Business", "Freelance", "Investment", "Other Income")
            .map { Category(name = it, type = TxType.INCOME, isDefault = true) }
        val expense = listOf(
            "Food and Dining", "Transportation", "Housing", "Utilities", "Shopping",
            "Health", "Entertainment", "Education", "Other Expenses"
        ).map { Category(name = it, type = TxType.EXPENSE, isDefault = true) }
        catDao.insertAll(income + expense)
    }
}
