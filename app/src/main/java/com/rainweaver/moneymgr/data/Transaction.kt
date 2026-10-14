package com.rainweaver.moneymgr.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TxType { INCOME, EXPENSE, TRANSFER }

/**
 * A single financial transaction.
 * Money is stored as integer minor units (cents) in [amountMinor] — never a
 * floating-point value — so sums and calculations stay exact.
 * [categoryName] is a historical snapshot so records stay valid even if the
 * category is later renamed or removed.
 */
@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: TxType,
    val amountMinor: Long,
    val categoryId: Long,
    val categoryName: String,
    val dateEpochDay: Long,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
