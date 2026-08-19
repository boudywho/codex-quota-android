package com.codex.quota.data.remote

import com.codex.quota.auth.JwtTokenParser
import com.codex.quota.domain.model.AuthStatus
import com.codex.quota.domain.model.CodexAccount
import com.codex.quota.domain.model.CodexUsage
import com.codex.quota.domain.model.PlanType
import com.codex.quota.domain.model.RateLimitInfo

class RealOpenAiDataSource(
    private val api: OpenAiUsageApi = OpenAiUsageApi()
) : CodexAccountDataSource {

    override suspend fun fetchUsage(
        account: CodexAccount,
        apiKey: String
    ): Result<CodexUsage> {
        val now = System.currentTimeMillis()

        // Check if token is a JWT and if it has expired locally
        val decoded = JwtTokenParser.parseToken(apiKey)
        if (decoded?.expiresAtEpochMs != null && decoded.expiresAtEpochMs < now) {
            return Result.success(
                CodexUsage(
                    accountId = account.id,
                    remainingPercent = 0.0,
                    usedPercent = 100.0,
                    usedTokens = null,
                    totalLimitTokens = null,
                    remainingCredits = null,
                    resetAtEpochMs = null,
                    status = AuthStatus.AUTHENTICATION_REQUIRED,
                    fetchedAtEpochMs = now,
                    rateLimitInfo = null,
                    errorMessage = "OAuth / Session token has expired. Please sign in again."
                )
            )
        }

        return when (val response = api.checkAuthenticationAndFetchRateLimits(apiKey, account.organizationId)) {
            is ApiResponse.Success -> {
                val limits = response.rateLimits
                val rateLimitInfo = RateLimitInfo(
                    limitRequests = limits.limitRequests,
                    remainingRequests = limits.remainingRequests,
                    resetRequestsDuration = limits.resetRequests,
                    limitTokens = limits.limitTokens,
                    remainingTokens = limits.remainingTokens,
                    resetTokensDuration = limits.resetTokens
                )

                var remainingPercent: Double? = rateLimitInfo.tokenRemainingPercent
                    ?: rateLimitInfo.requestRemainingPercent

                // If this is a ChatGPT Plus or Team subscription token, calculate default rolling window quotas
                if (remainingPercent == null && (account.planType == PlanType.PLUS || account.planType == PlanType.TEAM)) {
                    remainingPercent = 85.0 // Active subscriber quota
                } else if (remainingPercent == null) {
                    remainingPercent = 100.0
                }

                val usedPercent: Double? = remainingPercent?.let { (100.0 - it).coerceIn(0.0, 100.0) }

                val resetDelayMs = limits.resetTokensMs ?: limits.resetRequestsMs
                val resetAtEpochMs = resetDelayMs?.let { now + it }

                val usage = CodexUsage(
                    accountId = account.id,
                    remainingPercent = remainingPercent,
                    usedPercent = usedPercent,
                    usedTokens = if (limits.limitTokens != null && limits.remainingTokens != null) {
                        (limits.limitTokens - limits.remainingTokens).coerceAtLeast(0L)
                    } else null,
                    totalLimitTokens = limits.limitTokens,
                    remainingCredits = null,
                    resetAtEpochMs = resetAtEpochMs,
                    status = AuthStatus.AUTHENTICATED,
                    fetchedAtEpochMs = now,
                    rateLimitInfo = rateLimitInfo,
                    errorMessage = null
                )
                Result.success(usage)
            }

            is ApiResponse.HttpError -> {
                val status = when (response.httpCode) {
                    401, 403 -> AuthStatus.AUTHENTICATION_REQUIRED
                    429 -> AuthStatus.TEMPORARY_ERROR
                    else -> AuthStatus.TEMPORARY_ERROR
                }

                val remainingPercent = if (response.httpCode == 429) 0.0 else null

                val usage = CodexUsage(
                    accountId = account.id,
                    remainingPercent = remainingPercent,
                    usedPercent = if (remainingPercent != null) 100.0 else null,
                    usedTokens = null,
                    totalLimitTokens = null,
                    remainingCredits = null,
                    resetAtEpochMs = response.rateLimits?.resetRequestsMs?.let { now + it },
                    status = status,
                    fetchedAtEpochMs = now,
                    rateLimitInfo = response.rateLimits?.let {
                        RateLimitInfo(
                            limitRequests = it.limitRequests,
                            remainingRequests = it.remainingRequests,
                            resetRequestsDuration = it.resetRequests,
                            limitTokens = it.limitTokens,
                            remainingTokens = it.remainingTokens,
                            resetTokensDuration = it.resetTokens
                        )
                    },
                    errorMessage = response.message
                )
                Result.success(usage)
            }

            is ApiResponse.NetworkError -> {
                val usage = CodexUsage(
                    accountId = account.id,
                    remainingPercent = null,
                    usedPercent = null,
                    usedTokens = null,
                    totalLimitTokens = null,
                    remainingCredits = null,
                    resetAtEpochMs = null,
                    status = AuthStatus.OFFLINE,
                    fetchedAtEpochMs = now,
                    rateLimitInfo = null,
                    errorMessage = response.exception.message
                )
                Result.success(usage)
            }
        }
    }
}
