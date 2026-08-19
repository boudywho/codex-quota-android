package com.codex.quota.auth

import android.util.Base64
import com.codex.quota.domain.model.PlanType
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

data class DecodedTokenInfo(
    val email: String?,
    val planType: PlanType,
    val expiresAtEpochMs: Long?,
    val userId: String?,
    val organizationId: String?
)

object JwtTokenParser {

    private val json = Json { ignoreUnknownKeys = true }

    fun parseToken(token: String): DecodedTokenInfo? {
        val cleanToken = token.trim().removePrefix("Bearer ").trim()
        val parts = cleanToken.split(".")
        if (parts.size < 2) {
            // Not a JWT token; might be opaque session token or API key
            return null
        }

        return try {
            val payloadBase64 = parts[1]
            val decodedBytes = Base64.decode(
                payloadBase64,
                Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP
            )
            val payloadString = String(decodedBytes, Charsets.UTF_8)
            val jsonObject = json.decodeFromString<JsonObject>(payloadString)

            val email = jsonObject["email"]?.jsonPrimitive?.content
                ?: jsonObject["https://api.openai.com/profile"]?.toString()

            val expSeconds = jsonObject["exp"]?.jsonPrimitive?.longOrNull
            val expEpochMs = expSeconds?.let { it * 1000L }

            val userId = jsonObject["sub"]?.jsonPrimitive?.content
                ?: jsonObject["user_id"]?.jsonPrimitive?.content

            val orgId = jsonObject["org_id"]?.jsonPrimitive?.content
                ?: jsonObject["https://api.openai.com/auth"]?.toString()

            // Infer plan type from token claims (e.g. chatgpt_plus, chatgpt_team, enterprise)
            val payloadLower = payloadString.lowercase()
            val planType = when {
                payloadLower.contains("team") -> PlanType.TEAM
                payloadLower.contains("enterprise") -> PlanType.ENTERPRISE
                payloadLower.contains("plus") || payloadLower.contains("chatgpt") -> PlanType.PLUS
                else -> PlanType.PLUS
            }

            DecodedTokenInfo(
                email = email,
                planType = planType,
                expiresAtEpochMs = expEpochMs,
                userId = userId,
                organizationId = orgId
            )
        } catch (e: Exception) {
            null
        }
    }
}
