package com.rainweaver.moneymgr.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A spending/income category. [isDefault] marks the built-in seeded ones. */
@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: TxType,
    val isDefault: Boolean = false
)
