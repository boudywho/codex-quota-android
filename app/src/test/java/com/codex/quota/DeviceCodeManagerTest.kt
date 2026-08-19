package com.codex.quota

import com.codex.quota.auth.DeviceCodeManager
import com.codex.quota.auth.DeviceCodeSession
import com.codex.quota.auth.DevicePollResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceCodeManagerTest {

    @Test
    fun generateFallbackUserCode_matchesStandardPattern() {
        val code = DeviceCodeManager.generateFallbackUserCode()
        assertNotNull(code)
        assertTrue(code.contains("-"))
        assertEquals(10, code.length) // 4 + 1 + 5 = 10 chars, e.g. ABCD-EFGH0
    }

    @Test
    fun requestDeviceCode_returnsValidSession() = runTest {
        val result = DeviceCodeManager.requestDeviceCode()
        assertTrue(result.isSuccess)
        val session = result.getOrThrow()
        assertNotNull(session.userCode)
        assertNotNull(session.deviceCode)
        assertEquals(DeviceCodeManager.VERIFICATION_URL, session.verificationUri)
        assertTrue(session.expiresInSeconds > 0)
    }

    @Test
    fun pollDeviceToken_expiredSession_returnsExpired() = runTest {
        val expiredSession = DeviceCodeSession(
            deviceCode = "expired_dev",
            userCode = "TEST-12345",
            verificationUri = DeviceCodeManager.VERIFICATION_URL,
            verificationUriComplete = null,
            expiresInSeconds = 0,
            intervalSeconds = 5,
            createdAtEpochMs = System.currentTimeMillis() - 10000L
        )

        val pollResult = DeviceCodeManager.pollDeviceToken(expiredSession)
        assertEquals(DevicePollResult.Expired, pollResult)
    }
}
