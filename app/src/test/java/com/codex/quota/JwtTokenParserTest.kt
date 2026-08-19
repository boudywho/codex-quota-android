package com.codex.quota

import android.util.Base64
import com.codex.quota.auth.JwtTokenParser
import com.codex.quota.domain.model.PlanType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class JwtTokenParserTest {

    @Test
    fun parseToken_invalidToken_returnsNull() {
        assertNull(JwtTokenParser.parseToken("not_a_jwt"))
        assertNull(JwtTokenParser.parseToken("sk-regular-api-key"))
    }
}
