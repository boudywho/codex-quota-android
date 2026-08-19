package com.codex.quota

import com.codex.quota.data.remote.MockOpenAiDataSource
import com.codex.quota.domain.model.AuthStatus
import com.codex.quota.domain.model.CodexAccount
import com.codex.quota.domain.model.PlanType
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MockDataSourceTest {

    private val mockDataSource = MockOpenAiDataSource()

    private fun createTestAccount(
        id: String,
        nickname: String,
        planType: PlanType,
        isDemo: Boolean = true
    ): CodexAccount {
        return CodexAccount(
            id = id,
            nickname = nickname,
            email = "user@test.org",
            planType = planType,
            organizationId = null,
            colorHex = "#10B981",
            authStatus = AuthStatus.UNKNOWN,
            isDemoAccount = isDemo,
            orderIndex = 0,
            createdAtEpochMs = System.currentTimeMillis(),
            lastSuccessfulSyncEpochMs = null
        )
    }

    @Test
    fun fetchUsage_personalPlusProfile_returnsValidUsage() = runTest {
        val account = createTestAccount("1", "Personal Account", PlanType.PLUS)
        val result = mockDataSource.fetchUsage(account, "mock_key")

        assertTrue(result.isSuccess)
        val usage = result.getOrThrow()
        assertEquals(78.0, usage.remainingPercent!!, 0.1)
        assertEquals(AuthStatus.AUTHENTICATED, usage.status)
        assertNotNull(usage.resetAtEpochMs)
    }

    @Test
    fun fetchUsage_workTeamProfile_returnsTeamUsage() = runTest {
        val account = createTestAccount("2", "Work Team", PlanType.TEAM)
        val result = mockDataSource.fetchUsage(account, "mock_team_key")

        assertTrue(result.isSuccess)
        val usage = result.getOrThrow()
        assertEquals(31.0, usage.remainingPercent!!, 0.1)
        assertEquals(AuthStatus.AUTHENTICATED, usage.status)
    }

    @Test
    fun fetchUsage_signedOutSimulator_returnsAuthRequired() = runTest {
        val account = createTestAccount("3", "Signed Out Account", PlanType.PLUS)
        val result = mockDataSource.fetchUsage(account, "mock_signed_out")

        assertTrue(result.isSuccess)
        val usage = result.getOrThrow()
        assertEquals(AuthStatus.AUTHENTICATION_REQUIRED, usage.status)
        assertNotNull(usage.errorMessage)
    }

    @Test
    fun fetchUsage_rateLimitSimulator_returnsTemporaryError() = runTest {
        val account = createTestAccount("4", "Rate Limit Simulator", PlanType.API_TIER_1)
        val result = mockDataSource.fetchUsage(account, "mock_rate_limit")

        assertTrue(result.isSuccess)
        val usage = result.getOrThrow()
        assertEquals(AuthStatus.TEMPORARY_ERROR, usage.status)
        assertEquals(0.0, usage.remainingPercent!!, 0.01)
    }
}
