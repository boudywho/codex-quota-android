package com.codex.quota.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.codex.quota.data.local.entity.UsageSnapshotEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UsageSnapshotDao {

    @Query("SELECT * FROM usage_snapshots")
    fun observeAll(): Flow<List<UsageSnapshotEntity>>

    @Query("SELECT * FROM usage_snapshots WHERE accountId = :accountId LIMIT 1")
    fun observeByAccountId(accountId: String): Flow<UsageSnapshotEntity?>

    @Query("SELECT * FROM usage_snapshots WHERE accountId = :accountId LIMIT 1")
    suspend fun getByAccountId(accountId: String): UsageSnapshotEntity?

    @Query("SELECT * FROM usage_snapshots")
    suspend fun getAll(): List<UsageSnapshotEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(snapshot: UsageSnapshotEntity)

    @Query("DELETE FROM usage_snapshots WHERE accountId = :accountId")
    suspend fun deleteByAccountId(accountId: String)

    @Query("DELETE FROM usage_snapshots")
    suspend fun deleteAll()
}
