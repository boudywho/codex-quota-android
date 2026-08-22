package com.codex.quota

import com.codex.quota.auth.DecodedTokenInfo
import com.codex.quota.auth.JwtTokenParser
import com.codex.quota.data.remote.ApiResponse
import com.codex.quota.data.remote.OpenAiUsageService
import com.codex.quota.data.remote.RealOpenAiDataSource
import com.codex.quota.data.remote.dto.ChatGptAccountCheckData
import com.codex.quota.data.remote.dto.ChatGptRateLimitResetCreditsDto
import com.codex.quota.data.remote.dto.ChatGptResetCreditDto
import com.codex.quota.data.remote.dto.ChatGptResetCreditsDto
import com.codex.quota.data.remote.dto.ChatGptWhamUsageDto
import com.codex.quota.data.remote.dto.OpenAiModelsResponseDto
import com.codex.quota.data.remote.dto.ParsedRateLimits
import com.codex.quota.domain.model.AuthStatus
import com.codex.quota.domain.model.CodexAccount
import com.codex.quota.domain.model.PlanType
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class RealOpenAiDataSourceTest {
    private val ignoreUnknownKeysJson = Json { ignoreUnknownKeys = true }

    @Test
    fun subscriberRequests_runConcurrentlyAndPreserveApiMetadataFallbacks() = runTest {
        val jwtRenewal = 9_000_000_000_000L
        val apiRenewal = 8_000_000_000_000L
        mockkObject(JwtTokenParser)
        every { JwtTokenParser.parseToken(any()) } returns DecodedTokenInfo(
            email = "person@example.com",
            userId = "user-1",
            organizationId = null,
            chatgptAccountId = "chatgpt-account-1",
            planType = PlanType.PLUS,
            expiresAtEpochMs = Long.MAX_VALUE,
            subscriptionExpiresAtEpochMs = jwtRenewal,
            subscriptionStartedAtEpochMs = 1_000L
        )

        try {
            val api = GatedSubscriberApi(apiRenewal)
            val fetch = async {
                RealOpenAiDataSource(api).fetchUsage(account(), "subscriber.jwt.token")
            }
            runCurrent()

            assertTrue(api.whamStarted.isCompleted)
            assertTrue(api.accountCheckStarted.isCompleted)
            assertTrue(api.resetCreditsStarted.isCompleted)
            api.releaseResponses.complete(Unit)

            val usage = fetch.await().getOrThrow()
            assertEquals(AuthStatus.AUTHENTICATED, usage.status)
            assertEquals(apiRenewal, usage.subscriptionRenewalEpochMs)
            assertEquals(1_000L, usage.subscriptionStartedAtEpochMs)
            assertEquals("Annual", usage.billingPeriod)
            assertEquals(2_000L, usage.accountCreatedEpochMs)
            assertFalse(usage.willAutoRenew!!)
            assertTrue(usage.hasActiveSubscription!!)
            assertEquals(null, usage.bankedResets)
        } finally {
            unmockkObject(JwtTokenParser)
        }
    }

    @Test
    fun successfulSubscriberUsage_mapsBankedCountAndEarliestAvailableExpiry() = runTest {
        mockkObject(JwtTokenParser)
        every { JwtTokenParser.parseToken(any()) } returns DecodedTokenInfo(
            email = "person@example.com",
            userId = "user-1",
            organizationId = null,
            chatgptAccountId = "chatgpt-account-1",
            planType = PlanType.PLUS,
            expiresAtEpochMs = Long.MAX_VALUE,
            subscriptionExpiresAtEpochMs = 9_000_000_000_000L,
            subscriptionStartedAtEpochMs = 1_000L
        )

        try {
            val api = GatedSubscriberApi(
                apiRenewal = 8_000_000_000_000L,
                bankedResets = 3,
                resetCredits = listOf(
                    ChatGptResetCreditDto(
                        status = "available",
                        expiresAt = "2026-09-20T22:11:08.943280Z"
                    ),
                    ChatGptResetCreditDto(
                        status = "available",
                        expiresAt = "2026-09-19T10:00:00Z"
                    ),
                    ChatGptResetCreditDto(
                        status = "used",
                        expiresAt = "2026-09-01T10:00:00Z"
                    ),
                    ChatGptResetCreditDto(status = "available", expiresAt = "not-a-date")
                )
            )
            val fetch = async {
                RealOpenAiDataSource(api).fetchUsage(account(), "subscriber.jwt.token")
            }
            runCurrent()

            assertTrue(api.whamStarted.isCompleted)
            assertTrue(api.accountCheckStarted.isCompleted)
            assertTrue(api.resetCreditsStarted.isCompleted)
            api.releaseResponses.complete(Unit)

            val usage = fetch.await().getOrThrow()
            assertEquals(3, usage.bankedResets)
            assertEquals(
                Instant.parse("2026-09-19T10:00:00Z").toEpochMilli(),
                usage.bankedResetExpiresAtEpochMs
            )
        } finally {
            unmockkObject(JwtTokenParser)
        }
    }

    @Test
    fun resetCreditDetailsFailure_keepsSuccessfulUsageAndBankedCount() = runTest {
        mockkObject(JwtTokenParser)
        every { JwtTokenParser.parseToken(any()) } returns DecodedTokenInfo(
            email = null,
            userId = "user-1",
            organizationId = null,
            chatgptAccountId = "chatgpt-account-1",
            planType = PlanType.PLUS,
            expiresAtEpochMs = Long.MAX_VALUE,
            subscriptionExpiresAtEpochMs = null,
            subscriptionStartedAtEpochMs = null
        )

        try {
            val api = GatedSubscriberApi(
                apiRenewal = 8_000_000_000_000L,
                bankedResets = 2,
                throwResetCreditsFailure = true
            )
            val fetch = async {
                RealOpenAiDataSource(api).fetchUsage(account(), "subscriber.jwt.token")
            }
            runCurrent()
            api.releaseResponses.complete(Unit)

            val result = fetch.await()
            assertTrue(result.isSuccess)
            assertEquals(2, result.getOrThrow().bankedResets)
            assertEquals(null, result.getOrThrow().bankedResetExpiresAtEpochMs)
        } finally {
            unmockkObject(JwtTokenParser)
        }
    }

    @Test
    fun whamDto_decodesBankedResetsAndIgnoresUnknownFields() {
        val dto = ignoreUnknownKeysJson.decodeFromString<ChatGptWhamUsageDto>(
            """{"rate_limit_reset_credits":{"available_count":1,"future_field":true},"unknown_root":"value"}"""
        )

        assertEquals(1, dto.rateLimitResetCredits?.availableCount)
    }

    @Test
    fun resetCreditsDto_decodesTypedExpiryDetailsAndIgnoresUnknownFields() {
        val dto = ignoreUnknownKeysJson.decodeFromString<ChatGptResetCreditsDto>(
            """{"available_count":2,"credits":[{"status":"available","reset_type":"weekly","granted_at":"2026-08-20T10:00:00Z","expires_at":"2026-09-20T22:11:08.943280Z","title":"Banked reset","unknown_credit_field":42}],"unknown_root":true}"""
        )

        assertEquals(2, dto.availableCount)
        assertEquals("available", dto.credits.single().status)
        assertEquals("weekly", dto.credits.single().resetType)
        assertEquals("2026-08-20T10:00:00Z", dto.credits.single().grantedAt)
        assertEquals("2026-09-20T22:11:08.943280Z", dto.credits.single().expiresAt)
        assertEquals("Banked reset", dto.credits.single().title)
    }

    private fun account() = CodexAccount(
        id = "account-1",
        nickname = "Subscriber",
        email = null,
        planType = PlanType.PLUS,
        organizationId = null,
        colorHex = "#10B981",
        authStatus = AuthStatus.AUTHENTICATED,
        isDemoAccount = false,
        orderIndex = 0,
        createdAtEpochMs = 0L,
        lastSuccessfulSyncEpochMs = null
    )

    private class GatedSubscriberApi(
        private val apiRenewal: Long,
        private val bankedResets: Int? = null,
        private val resetCredits: List<ChatGptResetCreditDto> = emptyList(),
        private val throwResetCreditsFailure: Boolean = false
    ) : OpenAiUsageService {
        val whamStarted = CompletableDeferred<Unit>()
        val accountCheckStarted = CompletableDeferred<Unit>()
        val resetCreditsStarted = CompletableDeferred<Unit>()
        val releaseResponses = CompletableDeferred<Unit>()

        override suspend fun fetchChatGptSubscriberUsage(
            accessToken: String,
            chatgptAccountId: String?
        ): ApiResponse<ChatGptWhamUsageDto> {
            whamStarted.complete(Unit)
            releaseResponses.await()
            return if (bankedResets != null) {
                ApiResponse.Success(
                    data = ChatGptWhamUsageDto(
                        rateLimitResetCredits = ChatGptRateLimitResetCreditsDto(
                            availableCount = bankedResets
                        )
                    ),
                    rateLimits = emptyRateLimits(),
                    httpCode = 200
                )
            } else {
                ApiResponse.HttpError(503, "temporary outage", null)
            }
        }

        override suspend fun fetchChatGptAccountCheck(
            accessToken: String,
            chatgptAccountId: String?
        ): ApiResponse<ChatGptAccountCheckData> {
            accountCheckStarted.complete(Unit)
            releaseResponses.await()
            return ApiResponse.Success(
                data = ChatGptAccountCheckData(
                    accountCreatedEpochMs = 2_000L,
                    subscriptionRenewsEpochMs = apiRenewal,
                    subscriptionExpiresEpochMs = apiRenewal,
                    billingPeriod = "Annual",
                    willRenew = false,
                    hasActiveSubscription = true,
                    planType = "plus"
                ),
                rateLimits = emptyRateLimits(),
                httpCode = 200
            )
        }

        override suspend fun fetchChatGptResetCredits(
            accessToken: String,
            chatgptAccountId: String?
        ): ApiResponse<ChatGptResetCreditsDto> {
            resetCreditsStarted.complete(Unit)
            releaseResponses.await()
            if (throwResetCreditsFailure) throw IOException("details unavailable")
            return ApiResponse.Success(
                data = ChatGptResetCreditsDto(
                    availableCount = resetCredits.size,
                    credits = resetCredits
                ),
                rateLimits = emptyRateLimits(),
                httpCode = 200
            )
        }

        override suspend fun checkAuthenticationAndFetchRateLimits(
            apiKey: String,
            organizationId: String?
        ): ApiResponse<OpenAiModelsResponseDto> = error("Not used for subscriber tokens")

        private fun emptyRateLimits() = ParsedRateLimits(
            limitRequests = null,
            remainingRequests = null,
            resetRequests = null,
            resetRequestsMs = null,
            limitTokens = null,
            remainingTokens = null,
            resetTokens = null,
            resetTokensMs = null
        )
    }
}
