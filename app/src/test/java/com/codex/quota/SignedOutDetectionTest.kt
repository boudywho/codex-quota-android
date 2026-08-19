package com.codex.quota

import com.codex.quota.domain.model.AccountWithUsage
import com.codex.quota.domain.model.AuthStatus
import com.codex.quota.domain.model.CodexAccount
import com.codex.quota.domain.model.CodexUsage
import com.codex.quota.domain.model.PlanType
import com.codex.quota.domain.usecase.CheckSignedOutAccountsUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SignedOutDetectionTest {

    private val checkSignedOutUseCase = CheckSignedOutAccountsUseCase()

    private fun createAccount(id: String, status: AuthStatus): CodexAccount {
        return CodexAccount(
            id = id,
            nickname = "Account $id",
            email = null,
            planType = PlanType.PLUS,
            organizationId = null,
            colorHex = "#10B981",
            authStatus = status,
            isDemoAccount = true,
            orderIndex = 0,
            createdAtEpochMs = 1000L,
            lastSuccessfulSyncEpochMs = null
        )
    }

    @Test
    fun checkSignedOut_flagsSignedOutAccounts() {
        val acc1 = createAccount("1", AuthStatus.AUTHENTICATED)
        val acc2 = createAccount("2", AuthStatus.AUTHENTICATION_REQUIRED)

        val accounts = listOf(
            AccountWithUsage(acc1, CodexUsage.empty("1", AuthStatus.AUTHENTICATED)),
            AccountWithUsage(acc2, CodexUsage.empty("2", AuthStatus.AUTHENTICATION_REQUIRED))
        )

        val signedOut = checkSignedOutUseCase(accounts, previouslyAlertedIds = emptySet())

        assertEquals(1, signedOut.size)
        assertEquals("2", signedOut[0].id)
    }

    @Test
    fun checkSignedOut_deduplicatesAlreadyAlertedAccounts() {
        val acc1 = createAccount("1", AuthStatus.AUTHENTICATION_REQUIRED)
        val accounts = listOf(
            AccountWithUsage(acc1, CodexUsage.empty("1", AuthStatus.AUTHENTICATION_REQUIRED))
        )

        val signedOut = checkSignedOutUseCase(accounts, previouslyAlertedIds = setOf("1"))

        assertTrue(signedOut.isEmpty())
    }
}
