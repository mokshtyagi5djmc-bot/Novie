package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [CashFlowReportEntity::class, CashFlowEntryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class NovieDatabase : RoomDatabase() {
    abstract fun cashFlowDao(): CashFlowDao

    companion object {
        @Volatile
        private var INSTANCE: NovieDatabase? = null

        fun getInstance(context: Context): NovieDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    NovieDatabase::class.java,
                    "novie_cashflow.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
