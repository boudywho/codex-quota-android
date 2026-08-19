package com.codex.quota.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.codex.quota.data.local.dao.AccountDao
import com.codex.quota.data.local.dao.UsageSnapshotDao
import com.codex.quota.data.local.entity.AccountEntity
import com.codex.quota.data.local.entity.UsageSnapshotEntity

@Database(
    entities = [
        AccountEntity::class,
        UsageSnapshotEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun accountDao(): AccountDao
    abstract fun usageSnapshotDao(): UsageSnapshotDao

    companion object {
        private const val DATABASE_NAME = "codex_quota_db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
