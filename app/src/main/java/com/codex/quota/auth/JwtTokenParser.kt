package com.codex.quota.auth

import android.util.Base64
import com.codex.quota.domain.model.PlanType
import org.json.JSONObject
import java.nio.charset.StandardCharsets

data class DecodedTokenInfo(
    val email: String?,
    val userId: String?,
    val organizationId: String?,
    val chatgptAccountId: String? = null,
    val planType: PlanType = PlanType.PLUS,
    val expiresAtEpochMs: Long? = null,
    val name: String? = null,
    val rawClaims: Map<String, Any?> = emptyMap()
)

object JwtTokenParser {

    fun parseToken(token: String): DecodedTokenInfo? {
        val cleanToken = token.trim().removePrefix("Bearer ").removePrefix("bearer ")
        val parts = cleanToken.split(".")
        if (parts.size < 2) {
            return null
        }

        try {
            val payloadBytes = decodeBase64Url(parts[1]) ?: return null
            val payloadJson = String(payloadBytes, StandardCharsets.UTF_8)
            val json = JSONObject(payloadJson)

            var email = json.optString("email").takeIf { it.isNotEmpty() }
            var userId = json.optString("sub").takeIf { it.isNotEmpty() }
            var planType = PlanType.PLUS
            var orgId: String? = null
            var chatgptAccountId: String? = null
            var name = json.optString("name").takeIf { it.isNotEmpty() }

            val expSeconds = json.optLong("exp", 0L)
            val expiresAtEpochMs = if (expSeconds > 0) expSeconds * 1000L else null

            // Parse OpenAI specific auth claims: "https://api.openai.com/auth"
            val openAiAuth = json.optJSONObject("https://api.openai.com/auth")
            if (openAiAuth != null) {
                val planStr = openAiAuth.optString("chatgpt_plan_type", "plus")
                planType = when (planStr.lowercase()) {
                    "plus" -> PlanType.PLUS
                    "team" -> PlanType.TEAM
                    "enterprise" -> PlanType.ENTERPRISE
                    else -> PlanType.PLUS
                }

                if (openAiAuth.has("chatgpt_account_id")) {
                    chatgptAccountId = openAiAuth.optString("chatgpt_account_id").takeIf { it.isNotEmpty() }
                }

                if (openAiAuth.has("poid")) {
                    orgId = openAiAuth.optString("poid").takeIf { it.isNotEmpty() }
                }

                if (openAiAuth.has("organizations")) {
                    val orgsArray = openAiAuth.optJSONArray("organizations")
                    if (orgsArray != null && orgsArray.length() > 0) {
                        val firstOrg = orgsArray.getJSONObject(0)
                        if (orgId == null) {
                            orgId = firstOrg.optString("id").takeIf { it.isNotEmpty() }
                        }
                    }
                }

                if (openAiAuth.has("chatgpt_user_id")) {
                    userId = openAiAuth.optString("chatgpt_user_id")
                }
            }

            // Parse profile claims: "https://api.openai.com/profile"
            val openAiProfile = json.optJSONObject("https://api.openai.com/profile")
            if (openAiProfile != null) {
                if (email == null) {
                    email = openAiProfile.optString("email").takeIf { it.isNotEmpty() }
                }
                if (name == null) {
                    name = openAiProfile.optString("name").takeIf { it.isNotEmpty() }
                }
            }

            return DecodedTokenInfo(
                email = email,
                userId = userId,
                organizationId = orgId,
                chatgptAccountId = chatgptAccountId,
                planType = planType,
                expiresAtEpochMs = expiresAtEpochMs,
                name = name
            )
        } catch (e: Exception) {
            return null
        }
    }

    private fun decodeBase64Url(input: String): ByteArray? {
        return try {
            Base64.decode(input, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        } catch (e: Exception) {
            try {
                var padded = input
                while (padded.length % 4 != 0) {
                    padded += "="
                }
                Base64.decode(padded, Base64.DEFAULT)
            } catch (e2: Exception) {
                null
            }
        }
    }
}
