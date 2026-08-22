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
    val bankedResets: Int?,
    val bankedResetExpiresAtEpochMs: Long?,
    val resetAtEpochMs: Long?,
    val status: String,
    val fetchedAtEpochMs: Long,
    val limitRequests: Long?,
    val remainingRequests: Long?,
    val resetRequestsDuration: String?,
    val limitTokens: Long?,
    val remainingTokens: Long?,
    val resetTokensDuration: String?,
    val errorMessage: String?,
    val subscriptionRenewalEpochMs: Long?,
    val subscriptionStartedAtEpochMs: Long?,
    val billingPeriod: String?,
    val accountCreatedEpochMs: Long?,
    val willAutoRenew: Boolean?,
    val hasActiveSubscription: Boolean?
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
            bankedResets = bankedResets,
            bankedResetExpiresAtEpochMs = bankedResetExpiresAtEpochMs,
            resetAtEpochMs = resetAtEpochMs,
            status = try {
                AuthStatus.valueOf(status)
            } catch (e: Exception) {
                AuthStatus.UNKNOWN
            },
            fetchedAtEpochMs = fetchedAtEpochMs,
            rateLimitInfo = rateLimitInfo,
            errorMessage = errorMessage,
            subscriptionRenewalEpochMs = subscriptionRenewalEpochMs,
            subscriptionStartedAtEpochMs = subscriptionStartedAtEpochMs,
            billingPeriod = billingPeriod,
            accountCreatedEpochMs = accountCreatedEpochMs,
            willAutoRenew = willAutoRenew,
            hasActiveSubscription = hasActiveSubscription
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
                bankedResets = usage.bankedResets,
                bankedResetExpiresAtEpochMs = usage.bankedResetExpiresAtEpochMs,
                resetAtEpochMs = usage.resetAtEpochMs,
                status = usage.status.name,
                fetchedAtEpochMs = usage.fetchedAtEpochMs,
                limitRequests = usage.rateLimitInfo?.limitRequests,
                remainingRequests = usage.rateLimitInfo?.remainingRequests,
                resetRequestsDuration = usage.rateLimitInfo?.resetRequestsDuration,
                limitTokens = usage.rateLimitInfo?.limitTokens,
                remainingTokens = usage.rateLimitInfo?.remainingTokens,
                resetTokensDuration = usage.rateLimitInfo?.resetTokensDuration,
                errorMessage = usage.errorMessage,
                subscriptionRenewalEpochMs = usage.subscriptionRenewalEpochMs,
                subscriptionStartedAtEpochMs = usage.subscriptionStartedAtEpochMs,
                billingPeriod = usage.billingPeriod,
                accountCreatedEpochMs = usage.accountCreatedEpochMs,
                willAutoRenew = usage.willAutoRenew,
                hasActiveSubscription = usage.hasActiveSubscription
            )
        }
    }
}
