package com.rainweaver.moneymgr.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

class Converters {
    @TypeConverter fun typeToDb(v: TxType): String = v.name
    @TypeConverter fun dbToType(v: String): TxType = TxType.valueOf(v)
}

@Database(entities = [Transaction::class, Category::class], version = 2, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "money_manager.db"
                )
                    // Pre-release: schema is still evolving, so a version bump
                    // rebuilds cleanly. Before public ship, replace with explicit
                    // Migration objects (v1 -> v2 -> ...) to preserve user data.
                    .fallbackToDestructiveMigration()
                    .build().also { INSTANCE = it }
            }
    }
}
