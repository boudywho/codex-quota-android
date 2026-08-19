package com.codex.quota

import com.codex.quota.data.remote.dto.ParsedRateLimits
import okhttp3.Headers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class RateLimitHeaderParserTest {

    @Test
    fun parseStandardRateLimitHeaders_extractsAllValues() {
        val headers = Headers.Builder()
            .add("x-ratelimit-limit-requests", "5000")
            .add("x-ratelimit-remaining-requests", "4820")
            .add("x-ratelimit-reset-requests", "1s")
            .add("x-ratelimit-limit-tokens", "160000")
            .add("x-ratelimit-remaining-tokens", "120000")
            .add("x-ratelimit-reset-tokens", "2m30s")
            .build()

        val parsed = ParsedRateLimits.fromHeaders(headers)

        assertEquals(5000L, parsed.limitRequests)
        assertEquals(4820L, parsed.remainingRequests)
        assertEquals("1s", parsed.resetRequests)
        assertEquals(1000L, parsed.resetRequestsMs)

        assertEquals(160000L, parsed.limitTokens)
        assertEquals(120000L, parsed.remainingTokens)
        assertEquals("2m30s", parsed.resetTokens)
        assertEquals(150000L, parsed.resetTokensMs) // 2 * 60s + 30s = 150,000 ms
    }

    @Test
    fun parseDurationToMillis_supportsDifferentUnits() {
        assertEquals(20L, ParsedRateLimits.parseDurationToMillis("20ms"))
        assertEquals(5000L, ParsedRateLimits.parseDurationToMillis("5s"))
        assertEquals(60000L, ParsedRateLimits.parseDurationToMillis("1m"))
        assertEquals(7200000L, ParsedRateLimits.parseDurationToMillis("2h"))
        assertEquals(86400000L, ParsedRateLimits.parseDurationToMillis("1d"))
        assertEquals(125000L, ParsedRateLimits.parseDurationToMillis("2m5s"))
        assertNull(ParsedRateLimits.parseDurationToMillis(null))
        assertNull(ParsedRateLimits.parseDurationToMillis(""))
    }
}
