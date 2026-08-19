package com.codex.quota.domain.model

data class CodexAccount(
    val id: String,
    val nickname: String,
    val email: String?,
    val planType: PlanType,
    val organizationId: String?,
    val colorHex: String,
    val authStatus: AuthStatus,
    val isDemoAccount: Boolean,
    val orderIndex: Int,
    val createdAtEpochMs: Long,
    val lastSuccessfulSyncEpochMs: Long?
)
