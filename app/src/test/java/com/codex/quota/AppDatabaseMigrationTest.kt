package com.codex.quota

import androidx.sqlite.db.SupportSQLiteDatabase
import com.codex.quota.data.local.AppDatabase
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class AppDatabaseMigrationTest {

    @Test
    fun migration4To5_addsNullableBankedResetExpiryColumn() {
        val database = mockk<SupportSQLiteDatabase>(relaxed = true)

        AppDatabase.MIGRATION_4_5.migrate(database)

        verify(exactly = 1) {
            database.execSQL(
                "ALTER TABLE usage_snapshots ADD COLUMN bankedResetExpiresAtEpochMs INTEGER"
            )
        }
    }
}
