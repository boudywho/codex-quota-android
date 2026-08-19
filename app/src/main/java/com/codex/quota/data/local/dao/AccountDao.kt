package com.codex.quota.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.codex.quota.data.local.entity.AccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {

    @Query("SELECT * FROM accounts ORDER BY orderIndex ASC, createdAtEpochMs ASC")
    fun observeAll(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE id = :accountId LIMIT 1")
    fun observeById(accountId: String): Flow<AccountEntity?>

    @Query("SELECT * FROM accounts WHERE id = :accountId LIMIT 1")
    suspend fun getById(accountId: String): AccountEntity?

    @Query("SELECT * FROM accounts ORDER BY orderIndex ASC, createdAtEpochMs ASC")
    suspend fun getAll(): List<AccountEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(account: AccountEntity)

    @Update
    suspend fun update(account: AccountEntity)

    @Query("UPDATE accounts SET nickname = :nickname, colorHex = :colorHex, customRenewalDateEpochMs = :customRenewalDateEpochMs WHERE id = :accountId")
    suspend fun updateDetails(accountId: String, nickname: String, colorHex: String, customRenewalDateEpochMs: Long?)

    @Query("UPDATE accounts SET nickname = :nickname, colorHex = :colorHex WHERE id = :accountId")
    suspend fun updateNicknameAndColor(accountId: String, nickname: String, colorHex: String)

    @Query("UPDATE accounts SET customRenewalDateEpochMs = :renewalDateEpochMs WHERE id = :accountId")
    suspend fun updateRenewalDate(accountId: String, renewalDateEpochMs: Long?)

    @Query("UPDATE accounts SET authStatus = :authStatus, lastSuccessfulSyncEpochMs = :lastSync WHERE id = :accountId")
    suspend fun updateAuthStatusAndSyncTime(accountId: String, authStatus: String, lastSync: Long?)

    @Query("DELETE FROM accounts WHERE id = :accountId")
    suspend fun deleteById(accountId: String)

    @Query("DELETE FROM accounts")
    suspend fun deleteAll()

    @Transaction
    suspend fun updateOrderIndices(orderedIds: List<String>) {
        orderedIds.forEachIndexed { index, id ->
            updateOrderIndex(id, index)
        }
    }

    @Query("UPDATE accounts SET orderIndex = :orderIndex WHERE id = :id")
    suspend fun updateOrderIndex(id: String, orderIndex: Int)
}
