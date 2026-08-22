package com.codex.quota

import com.codex.quota.auth.DecodedTokenInfo
import com.codex.quota.auth.JwtTokenParser
import com.codex.quota.data.remote.ApiResponse
import com.codex.quota.data.remote.OpenAiUsageService
import com.codex.quota.data.remote.RealOpenAiDataSource
import com.codex.quota.data.remote.dto.ChatGptAccountCheckData
import com.codex.quota.data.remote.dto.ChatGptRateLimitResetCreditsDto
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
    fun successfulSubscriberUsage_mapsBankedResetsFromWhamDto() = runTest {
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
                bankedResets = 1
            )
            val fetch = async {
                RealOpenAiDataSource(api).fetchUsage(account(), "subscriber.jwt.token")
            }
            runCurrent()

            assertTrue(api.whamStarted.isCompleted)
            assertTrue(api.accountCheckStarted.isCompleted)
            api.releaseResponses.complete(Unit)

            assertEquals(1, fetch.await().getOrThrow().bankedResets)
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
        private val bankedResets: Int? = null
    ) : OpenAiUsageService {
        val whamStarted = CompletableDeferred<Unit>()
        val accountCheckStarted = CompletableDeferred<Unit>()
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
