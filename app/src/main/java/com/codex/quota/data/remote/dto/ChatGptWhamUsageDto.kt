package com.codex.quota.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChatGptWhamUsageDto(
    @SerialName("user_id") val userId: String? = null,
    @SerialName("account_id") val accountId: String? = null,
    @SerialName("email") val email: String? = null,
    @SerialName("plan_type") val planType: String? = null,
    @SerialName("rate_limit") val rateLimit: ChatGptRateLimitDto? = null,
    @SerialName("credits") val credits: ChatGptCreditsDto? = null
)

@Serializable
data class ChatGptRateLimitDto(
    @SerialName("allowed") val allowed: Boolean = true,
    @SerialName("limit_reached") val limitReached: Boolean = false,
    @SerialName("primary_window") val primaryWindow: ChatGptWindowDto? = null,
    @SerialName("secondary_window") val secondaryWindow: ChatGptWindowDto? = null
)

@Serializable
data class ChatGptWindowDto(
    @SerialName("used_percent") val usedPercent: Double? = null,
    @SerialName("limit_window_seconds") val limitWindowSeconds: Long? = null,
    @SerialName("reset_after_seconds") val resetAfterSeconds: Long? = null,
    @SerialName("reset_at") val resetAt: Long? = null
)

@Serializable
data class ChatGptCreditsDto(
    @SerialName("has_credits") val hasCredits: Boolean = false,
    @SerialName("unlimited") val unlimited: Boolean = false,
    @SerialName("balance") val balance: String? = null
)
