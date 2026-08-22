package com.codex.quota.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChatGptResetCreditsDto(
    @SerialName("available_count") val availableCount: Int? = null,
    val credits: List<ChatGptResetCreditDto> = emptyList()
)

@Serializable
data class ChatGptResetCreditDto(
    val status: String? = null,
    @SerialName("reset_type") val resetType: String? = null,
    @SerialName("granted_at") val grantedAt: String? = null,
    @SerialName("expires_at") val expiresAt: String? = null,
    val title: String? = null
)
