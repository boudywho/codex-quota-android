package com.codex.quota.data.remote

import com.codex.quota.domain.model.AuthStatus
import com.codex.quota.domain.model.CodexAccount
import com.codex.quota.domain.model.CodexUsage
import com.codex.quota.domain.model.PlanType
import com.codex.quota.domain.model.RateLimitInfo

class MockOpenAiDataSource : CodexAccountDataSource {

    override suspend fun fetchUsage(
        account: CodexAccount,
        apiKey: String
    ): Result<CodexUsage> {
        val now = System.currentTimeMillis()

        val usage = when {
            apiKey.contains("signed_out", ignoreCase = true) || account.nickname.contains("Signed Out", ignoreCase = true) -> {
                CodexUsage(
                    accountId = account.id,
                    remainingPercent = null,
                    usedPercent = null,
                    usedTokens = null,
                    totalLimitTokens = null,
                    remainingCredits = null,
                    resetAtEpochMs = null,
                    status = AuthStatus.AUTHENTICATION_REQUIRED,
                    fetchedAtEpochMs = now,
                    errorMessage = "Session token has expired or was revoked by OpenAI."
                )
            }

            apiKey.contains("rate_limit", ignoreCase = true) || account.nickname.contains("Rate Limit", ignoreCase = true) -> {
                CodexUsage(
                    accountId = account.id,
                    remainingPercent = 0.0,
                    usedPercent = 100.0,
                    usedTokens = 100_000L,
                    totalLimitTokens = 100_000L,
                    remainingCredits = 0.0,
                    resetAtEpochMs = now + (12 * 60 * 1000L), // 12 mins
                    status = AuthStatus.TEMPORARY_ERROR,
                    fetchedAtEpochMs = now,
                    rateLimitInfo = RateLimitInfo(
                        limitRequests = 500,
                        remainingRequests = 0,
                        resetRequestsDuration = "12m",
                        limitTokens = 100_000,
                        remainingTokens = 0,
                        resetTokensDuration = "12m"
                    ),
                    errorMessage = "Rate limit reached (429). Reset in 12 minutes."
                )
            }

            account.planType == PlanType.TEAM -> {
                CodexUsage(
                    accountId = account.id,
                    remainingPercent = 31.0,
                    usedPercent = 69.0,
                    usedTokens = 345_000L,
                    totalLimitTokens = 500_000L,
                    remainingCredits = 45.50,
                    resetAtEpochMs = now + (16 * 60 * 60 * 1000L), // 16 hours
                    status = AuthStatus.AUTHENTICATED,
                    fetchedAtEpochMs = now,
                    rateLimitInfo = RateLimitInfo(
                        limitRequests = 5000,
                        remainingRequests = 1550,
                        resetRequestsDuration = "16h",
                        limitTokens = 500_000,
                        remainingTokens = 155_000,
                        resetTokensDuration = "16h"
                    )
                )
            }

            account.planType == PlanType.ENTERPRISE || account.planType == PlanType.API_TIER_5 -> {
                CodexUsage(
                    accountId = account.id,
                    remainingPercent = 94.0,
                    usedPercent = 6.0,
                    usedTokens = 60_000L,
                    totalLimitTokens = 1_000_000L,
                    remainingCredits = 1250.00,
                    resetAtEpochMs = now + (28 * 60 * 60 * 1000L),
                    status = AuthStatus.AUTHENTICATED,
                    fetchedAtEpochMs = now,
                    rateLimitInfo = RateLimitInfo(
                        limitRequests = 10000,
                        remainingRequests = 9400,
                        resetRequestsDuration = "28h",
                        limitTokens = 1_000_000,
                        remainingTokens = 940_000,
                        resetTokensDuration = "28h"
                    )
                )
            }

            else -> {
                // Default: Personal Plus profile (78% remaining)
                CodexUsage(
                    accountId = account.id,
                    remainingPercent = 78.0,
                    usedPercent = 22.0,
                    usedTokens = 22_000L,
                    totalLimitTokens = 100_000L,
                    remainingCredits = null,
                    resetAtEpochMs = now + (2 * 3600 * 1000L + 15 * 60 * 1000L), // 2h 15m
                    status = AuthStatus.AUTHENTICATED,
                    fetchedAtEpochMs = now,
                    rateLimitInfo = RateLimitInfo(
                        limitRequests = 500,
                        remainingRequests = 390,
                        resetRequestsDuration = "2h 15m",
                        limitTokens = 100_000,
                        remainingTokens = 78_000,
                        resetTokensDuration = "2h 15m"
                    )
                )
            }
        }

        return Result.success(usage)
    }
}
