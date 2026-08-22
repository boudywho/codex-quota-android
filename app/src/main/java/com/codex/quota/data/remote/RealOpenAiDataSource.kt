package com.codex.quota.data.remote

import com.codex.quota.auth.JwtTokenParser
import com.codex.quota.domain.model.AuthStatus
import com.codex.quota.domain.model.CodexAccount
import com.codex.quota.domain.model.CodexUsage
import com.codex.quota.domain.model.RateLimitInfo
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class RealOpenAiDataSource(
    private val api: OpenAiUsageService = OpenAiUsageApi()
) : CodexAccountDataSource {

    override suspend fun fetchUsage(
        account: CodexAccount,
        apiKey: String
    ): Result<CodexUsage> {
        val now = System.currentTimeMillis()

        // Check if token is a ChatGPT Subscriber OAuth/Session JWT token
        val decoded = JwtTokenParser.parseToken(apiKey)
        if (decoded != null) {
            val isExpired = decoded.expiresAtEpochMs != null && decoded.expiresAtEpochMs < now
            if (isExpired) {
                val usage = CodexUsage(
                    accountId = account.id,
                    remainingPercent = 0.0,
                    usedPercent = 100.0,
                    usedTokens = null,
                    totalLimitTokens = null,
                    remainingCredits = null,
                    resetAtEpochMs = decoded.expiresAtEpochMs,
                    status = AuthStatus.AUTHENTICATION_REQUIRED,
                    fetchedAtEpochMs = now,
                    rateLimitInfo = null,
                    errorMessage = "ChatGPT session token has expired. Please re-authenticate.",
                    subscriptionRenewalEpochMs = decoded.subscriptionExpiresAtEpochMs,
                    subscriptionStartedAtEpochMs = decoded.subscriptionStartedAtEpochMs,
                    billingPeriod = "Monthly"
                )
                return Result.success(usage)
            }

            // Fetch live ChatGPT subscriber usage from chatgpt.com/backend-api/wham/usage
            val chatgptAccountId = decoded.chatgptAccountId ?: account.organizationId
            val (whamResponse, checkResponse) = coroutineScope {
                val whamRequest = async {
                    api.fetchChatGptSubscriberUsage(apiKey, chatgptAccountId)
                }
                val accountCheckRequest = async {
                    api.fetchChatGptAccountCheck(apiKey, chatgptAccountId)
                }
                whamRequest.await() to accountCheckRequest.await()
            }

            // Supplement usage with account and subscription entitlement details.
            val checkData = if (checkResponse is ApiResponse.Success) checkResponse.data else null

            val subRenewalEpochMs = checkData?.subscriptionRenewsEpochMs ?: decoded.subscriptionExpiresAtEpochMs
            val accountCreatedEpochMs = checkData?.accountCreatedEpochMs
            val billingPeriod = checkData?.billingPeriod ?: "Monthly"
            val willAutoRenew = checkData?.willRenew ?: true
            val hasActiveSubscription = checkData?.hasActiveSubscription ?: true

            when (whamResponse) {
                is ApiResponse.Success -> {
                    val whamDto = whamResponse.data
                    val rateLimit = whamDto.rateLimit
                    val primaryWindow = rateLimit?.primaryWindow
                    val secondaryWindow = rateLimit?.secondaryWindow

                    val usedPercent = primaryWindow?.usedPercent ?: secondaryWindow?.usedPercent ?: 0.0
                    val remainingPercent = (100.0 - usedPercent).coerceIn(0.0, 100.0)

                    val resetAtEpochMs = primaryWindow?.resetAt?.let { it * 1000L }
                        ?: secondaryWindow?.resetAt?.let { it * 1000L }
                        ?: decoded.expiresAtEpochMs

                    val isLimitReached = rateLimit?.limitReached == true || primaryWindow?.usedPercent?.let { it >= 100.0 } == true
                    val status = AuthStatus.AUTHENTICATED

                    val resetDurationFormatted = primaryWindow?.resetAfterSeconds?.let { sec ->
                        val hours = sec / 3600
                        val mins = (sec % 3600) / 60
                        if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
                    }

                    val rateLimitInfo = RateLimitInfo(
                        limitRequests = primaryWindow?.limitWindowSeconds?.toLong(),
                        remainingRequests = null,
                        resetRequestsDuration = resetDurationFormatted,
                        limitTokens = null,
                        remainingTokens = null,
                        resetTokensDuration = null
                    )

                    val creditsBalance = whamDto.credits?.balance?.toDoubleOrNull()

                    val usage = CodexUsage(
                        accountId = account.id,
                        remainingPercent = remainingPercent,
                        usedPercent = usedPercent,
                        usedTokens = null,
                        totalLimitTokens = null,
                        remainingCredits = creditsBalance,
                        resetAtEpochMs = resetAtEpochMs,
                        status = status,
                        fetchedAtEpochMs = now,
                        rateLimitInfo = rateLimitInfo,
                        errorMessage = if (isLimitReached) "Usage limit reached. Resets in $resetDurationFormatted" else null,
                        subscriptionRenewalEpochMs = subRenewalEpochMs,
                        subscriptionStartedAtEpochMs = decoded.subscriptionStartedAtEpochMs,
                        billingPeriod = billingPeriod,
                        accountCreatedEpochMs = accountCreatedEpochMs,
                        willAutoRenew = willAutoRenew,
                        hasActiveSubscription = hasActiveSubscription
                    )
                    return Result.success(usage)
                }

                is ApiResponse.HttpError -> {
                    if (whamResponse.httpCode == 401) {
                        val usage = CodexUsage(
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
                            errorMessage = "Session expired or revoked. Please re-authenticate.",
                            subscriptionRenewalEpochMs = subRenewalEpochMs,
                            subscriptionStartedAtEpochMs = decoded.subscriptionStartedAtEpochMs,
                            billingPeriod = billingPeriod,
                            accountCreatedEpochMs = accountCreatedEpochMs,
                            willAutoRenew = willAutoRenew,
                            hasActiveSubscription = hasActiveSubscription
                        )
                        return Result.success(usage)
                    }

                    // Fallback to active subscription window if transient HTTP error
                    val usage = CodexUsage(
                        accountId = account.id,
                        remainingPercent = 100.0,
                        usedPercent = 0.0,
                        usedTokens = null,
                        totalLimitTokens = null,
                        remainingCredits = null,
                        resetAtEpochMs = decoded.expiresAtEpochMs,
                        status = AuthStatus.AUTHENTICATED,
                        fetchedAtEpochMs = now,
                        rateLimitInfo = null,
                        errorMessage = whamResponse.message,
                        subscriptionRenewalEpochMs = subRenewalEpochMs,
                        subscriptionStartedAtEpochMs = decoded.subscriptionStartedAtEpochMs,
                        billingPeriod = billingPeriod,
                        accountCreatedEpochMs = accountCreatedEpochMs,
                        willAutoRenew = willAutoRenew,
                        hasActiveSubscription = hasActiveSubscription
                    )
                    return Result.success(usage)
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
                        errorMessage = whamResponse.exception.message,
                        subscriptionRenewalEpochMs = subRenewalEpochMs,
                        subscriptionStartedAtEpochMs = decoded.subscriptionStartedAtEpochMs,
                        billingPeriod = billingPeriod,
                        accountCreatedEpochMs = accountCreatedEpochMs,
                        willAutoRenew = willAutoRenew,
                        hasActiveSubscription = hasActiveSubscription
                    )
                    return Result.success(usage)
                }
            }
        }

        // Platform API Key (sk-...) validation & header rate limits
        return when (val response = api.checkAuthenticationAndFetchRateLimits(apiKey, account.organizationId)) {
            is ApiResponse.Success -> {
                val limits = response.rateLimits
                val rateLimitInfo = RateLimitInfo(
                    limitRequests = limits.limitRequests?.toLong(),
                    remainingRequests = limits.remainingRequests?.toLong(),
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
                            limitRequests = it.limitRequests?.toLong(),
                            remainingRequests = it.remainingRequests?.toLong(),
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
