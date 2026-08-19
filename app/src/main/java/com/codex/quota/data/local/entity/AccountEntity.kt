package com.codex.quota.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.codex.quota.domain.model.AuthStatus
import com.codex.quota.domain.model.CodexAccount
import com.codex.quota.domain.model.PlanType

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey
    val id: String,
    val nickname: String,
    val email: String?,
    val planType: String,
    val organizationId: String?,
    val colorHex: String,
    val authStatus: String,
    val isDemoAccount: Boolean,
    val orderIndex: Int,
    val createdAtEpochMs: Long,
    val lastSuccessfulSyncEpochMs: Long?
) {
    fun toDomain(): CodexAccount {
        return CodexAccount(
            id = id,
            nickname = nickname,
            email = email,
            planType = PlanType.fromString(planType),
            organizationId = organizationId,
            colorHex = colorHex,
            authStatus = try {
                AuthStatus.valueOf(authStatus)
            } catch (e: Exception) {
                AuthStatus.UNKNOWN
            },
            isDemoAccount = isDemoAccount,
            orderIndex = orderIndex,
            createdAtEpochMs = createdAtEpochMs,
            lastSuccessfulSyncEpochMs = lastSuccessfulSyncEpochMs
        )
    }

    companion object {
        fun fromDomain(account: CodexAccount): AccountEntity {
            return AccountEntity(
                id = account.id,
                nickname = account.nickname,
                email = account.email,
                planType = account.planType.name,
                organizationId = account.organizationId,
                colorHex = account.colorHex,
                authStatus = account.authStatus.name,
                isDemoAccount = account.isDemoAccount,
                orderIndex = account.orderIndex,
                createdAtEpochMs = account.createdAtEpochMs,
                lastSuccessfulSyncEpochMs = account.lastSuccessfulSyncEpochMs
            )
        }
    }
}
