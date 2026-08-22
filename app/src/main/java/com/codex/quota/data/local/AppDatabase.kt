package com.codex.quota.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.codex.quota.data.local.dao.AccountDao
import com.codex.quota.data.local.dao.UsageSnapshotDao
import com.codex.quota.data.local.entity.AccountEntity
import com.codex.quota.data.local.entity.UsageSnapshotEntity

@Database(
    entities = [
        AccountEntity::class,
        UsageSnapshotEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun accountDao(): AccountDao
    abstract fun usageSnapshotDao(): UsageSnapshotDao

    companion object {
        private const val DATABASE_NAME = "codex_quota_db"

        internal val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE accounts ADD COLUMN customRenewalDayOfMonth INTEGER")
                database.execSQL("ALTER TABLE accounts ADD COLUMN customRenewalDateEpochMs INTEGER")
            }
        }

        internal val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE usage_snapshots ADD COLUMN subscriptionRenewalEpochMs INTEGER")
                database.execSQL("ALTER TABLE usage_snapshots ADD COLUMN subscriptionStartedAtEpochMs INTEGER")
                database.execSQL("ALTER TABLE usage_snapshots ADD COLUMN billingPeriod TEXT")
                database.execSQL("ALTER TABLE usage_snapshots ADD COLUMN accountCreatedEpochMs INTEGER")
                database.execSQL("ALTER TABLE usage_snapshots ADD COLUMN willAutoRenew INTEGER")
                database.execSQL("ALTER TABLE usage_snapshots ADD COLUMN hasActiveSubscription INTEGER")
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
