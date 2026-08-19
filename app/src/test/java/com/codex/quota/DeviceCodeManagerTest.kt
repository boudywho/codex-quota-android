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
    fun deviceCodeSession_validProperties() {
        val session = DeviceCodeSession(
            deviceAuthId = "deviceauth_test_123",
            userCode = "0XV8-EZOKP",
            verificationUri = DeviceCodeManager.VERIFICATION_URL,
            expiresInSeconds = 900,
            intervalSeconds = 5
        )

        assertEquals("deviceauth_test_123", session.deviceAuthId)
        assertEquals("0XV8-EZOKP", session.userCode)
        assertEquals(DeviceCodeManager.VERIFICATION_URL, session.verificationUri)
        assertEquals(900, session.expiresInSeconds)
        assertEquals(5, session.intervalSeconds)
        assertTrue(session.userCode.contains("-"))
    }

    @Test
    fun pollDeviceToken_expiredSession_returnsExpired() = runTest {
        val expiredSession = DeviceCodeSession(
            deviceAuthId = "deviceauth_test",
            userCode = "TEST-12345",
            verificationUri = DeviceCodeManager.VERIFICATION_URL,
            expiresInSeconds = 0,
            intervalSeconds = 5,
            createdAtEpochMs = System.currentTimeMillis() - 10000L
        )

        val pollResult = DeviceCodeManager.pollDeviceToken(expiredSession)
        assertEquals(DevicePollResult.Expired, pollResult)
    }
}
