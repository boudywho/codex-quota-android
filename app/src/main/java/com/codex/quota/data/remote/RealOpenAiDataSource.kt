package com.codex.quota.data.remote

import com.codex.quota.domain.model.AuthStatus
import com.codex.quota.domain.model.CodexAccount
import com.codex.quota.domain.model.CodexUsage
import com.codex.quota.domain.model.RateLimitInfo

class RealOpenAiDataSource(
    private val api: OpenAiUsageApi = OpenAiUsageApi()
) : CodexAccountDataSource {

    override suspend fun fetchUsage(
        account: CodexAccount,
        apiKey: String
    ): Result<CodexUsage> {
        val now = System.currentTimeMillis()
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

                val remainingPercent: Double? = rateLimitInfo.tokenRemainingPercent
                    ?: rateLimitInfo.requestRemainingPercent
                    ?: 100.0

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
                    errorMessage = "Network unreachable. Check your connection."
                )
                Result.success(usage)
            }
        }
    }
}
