package com.codex.quota.domain.model

data class CodexUsage(
    val accountId: String,
    val remainingPercent: Double?,
    val usedPercent: Double?,
    val usedTokens: Long?,
    val totalLimitTokens: Long?,
    val remainingCredits: Double?,
    val resetAtEpochMs: Long?,
    val status: AuthStatus,
    val fetchedAtEpochMs: Long,
    val rateLimitInfo: RateLimitInfo? = null,
    val errorMessage: String? = null,
    val subscriptionRenewalEpochMs: Long? = null,
    val subscriptionStartedAtEpochMs: Long? = null,
    val billingPeriod: String? = null
) {
    val isStale: Boolean
        get() = (System.currentTimeMillis() - fetchedAtEpochMs) > STALE_THRESHOLD_MS

    companion object {
        const val STALE_THRESHOLD_MS = 30 * 60 * 1000L // 30 minutes

        fun empty(accountId: String, status: AuthStatus = AuthStatus.UNKNOWN): CodexUsage {
            return CodexUsage(
                accountId = accountId,
                remainingPercent = null,
                usedPercent = null,
                usedTokens = null,
                totalLimitTokens = null,
                remainingCredits = null,
                resetAtEpochMs = null,
                status = status,
                fetchedAtEpochMs = System.currentTimeMillis()
            )
        }
    }
}
