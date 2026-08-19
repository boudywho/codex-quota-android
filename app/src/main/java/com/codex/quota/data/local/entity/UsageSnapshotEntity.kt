package com.codex.quota.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.codex.quota.domain.model.AuthStatus
import com.codex.quota.domain.model.CodexUsage
import com.codex.quota.domain.model.RateLimitInfo

@Entity(
    tableName = "usage_snapshots",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["accountId"], unique = true)]
)
data class UsageSnapshotEntity(
    @PrimaryKey
    val accountId: String,
    val remainingPercent: Double?,
    val usedPercent: Double?,
    val usedTokens: Long?,
    val totalLimitTokens: Long?,
    val remainingCredits: Double?,
    val resetAtEpochMs: Long?,
    val status: String,
    val fetchedAtEpochMs: Long,
    val limitRequests: Long?,
    val remainingRequests: Long?,
    val resetRequestsDuration: String?,
    val limitTokens: Long?,
    val remainingTokens: Long?,
    val resetTokensDuration: String?,
    val errorMessage: String?
) {
    fun toDomain(): CodexUsage {
        val rateLimitInfo = if (limitRequests != null || limitTokens != null) {
            RateLimitInfo(
                limitRequests = limitRequests,
                remainingRequests = remainingRequests,
                resetRequestsDuration = resetRequestsDuration,
                limitTokens = limitTokens,
                remainingTokens = remainingTokens,
                resetTokensDuration = resetTokensDuration
            )
        } else {
            null
        }

        return CodexUsage(
            accountId = accountId,
            remainingPercent = remainingPercent,
            usedPercent = usedPercent,
            usedTokens = usedTokens,
            totalLimitTokens = totalLimitTokens,
            remainingCredits = remainingCredits,
            resetAtEpochMs = resetAtEpochMs,
            status = try {
                AuthStatus.valueOf(status)
            } catch (e: Exception) {
                AuthStatus.UNKNOWN
            },
            fetchedAtEpochMs = fetchedAtEpochMs,
            rateLimitInfo = rateLimitInfo,
            errorMessage = errorMessage
        )
    }

    companion object {
        fun fromDomain(usage: CodexUsage): UsageSnapshotEntity {
            return UsageSnapshotEntity(
                accountId = usage.accountId,
                remainingPercent = usage.remainingPercent,
                usedPercent = usage.usedPercent,
                usedTokens = usage.usedTokens,
                totalLimitTokens = usage.totalLimitTokens,
                remainingCredits = usage.remainingCredits,
                resetAtEpochMs = usage.resetAtEpochMs,
                status = usage.status.name,
                fetchedAtEpochMs = usage.fetchedAtEpochMs,
                limitRequests = usage.rateLimitInfo?.limitRequests,
                remainingRequests = usage.rateLimitInfo?.remainingRequests,
                resetRequestsDuration = usage.rateLimitInfo?.resetRequestsDuration,
                limitTokens = usage.rateLimitInfo?.limitTokens,
                remainingTokens = usage.rateLimitInfo?.remainingTokens,
                resetTokensDuration = usage.rateLimitInfo?.resetTokensDuration,
                errorMessage = usage.errorMessage
            )
        }
    }
}
