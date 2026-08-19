package com.codex.quota.domain.model

data class RateLimitInfo(
    val limitRequests: Long?,
    val remainingRequests: Long?,
    val resetRequestsDuration: String?,
    val limitTokens: Long?,
    val remainingTokens: Long?,
    val resetTokensDuration: String?
) {
    val requestRemainingPercent: Double?
        get() {
            if (limitRequests == null || remainingRequests == null || limitRequests <= 0) return null
            return ((remainingRequests.toDouble() / limitRequests.toDouble()) * 100.0).coerceIn(0.0, 100.0)
        }

    val tokenRemainingPercent: Double?
        get() {
            if (limitTokens == null || remainingTokens == null || limitTokens <= 0) return null
            return ((remainingTokens.toDouble() / limitTokens.toDouble()) * 100.0).coerceIn(0.0, 100.0)
        }
}
