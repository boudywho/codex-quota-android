package com.codex.quota.auth

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

data class DeviceCodeSession(
    val deviceAuthId: String,
    val userCode: String,
    val verificationUri: String = "https://auth.openai.com/codex/device",
    val expiresInSeconds: Int = 900,
    val intervalSeconds: Int = 5,
    val createdAtEpochMs: Long = System.currentTimeMillis()
)

sealed class DevicePollResult {
    data class Success(val tokenResult: OAuthTokenResult) : DevicePollResult()
    data object Pending : DevicePollResult()
    data object SlowDown : DevicePollResult()
    data class Error(val message: String) : DevicePollResult()
    data object Expired : DevicePollResult()
}

object DeviceCodeManager {

    const val USER_CODE_ENDPOINT = "https://auth.openai.com/api/accounts/deviceauth/usercode"
    const val TOKEN_ENDPOINT = "https://auth.openai.com/api/accounts/deviceauth/token"
    const val VERIFICATION_URL = "https://auth.openai.com/codex/device"

    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun requestDeviceCode(): Result<DeviceCodeSession> = withContext(Dispatchers.IO) {
        try {
            val emptyBody = "{}".toRequestBody(JSON_MEDIA_TYPE)
            val request = Request.Builder()
                .url(USER_CODE_ENDPOINT)
                .post(emptyBody)
                .addHeader("Accept", "application/json")
                .addHeader("User-Agent", "Codex-CLI/0.1.0 (Android; Mobile)")
                .build()

            val response = httpClient.newCall(request).execute()
            val body = response.body?.string().orEmpty()

            if (response.isSuccessful && body.isNotBlank()) {
                val json = JSONObject(body)
                val deviceAuthId = json.getString("device_auth_id")
                val userCode = json.getString("user_code")
                val interval = json.optString("interval", "5").toIntOrNull() ?: 5
                val expiresAtStr = json.optString("expires_at", "")

                var expiresInSeconds = 900
                if (expiresAtStr.isNotBlank()) {
                    try {
                        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
                            timeZone = TimeZone.getTimeZone("UTC")
                        }
                        val cleanDateStr = expiresAtStr.substringBefore('.').substringBefore('+').substringBefore('Z')
                        val expiryDate = sdf.parse(cleanDateStr)
                        if (expiryDate != null) {
                            val diff = (expiryDate.time - System.currentTimeMillis()) / 1000L
                            if (diff > 0) {
                                expiresInSeconds = diff.toInt()
                            }
                        }
                    } catch (e: Exception) {
                        expiresInSeconds = 900
                    }
                }

                return@withContext Result.success(
                    DeviceCodeSession(
                        deviceAuthId = deviceAuthId,
                        userCode = userCode,
                        verificationUri = VERIFICATION_URL,
                        expiresInSeconds = expiresInSeconds,
                        intervalSeconds = interval
                    )
                )
            } else {
                return@withContext Result.failure(Exception("Failed to request device code: ${response.code} $body"))
            }
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
    }

    suspend fun pollDeviceToken(session: DeviceCodeSession): DevicePollResult = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        if (now - session.createdAtEpochMs > session.expiresInSeconds * 1000L) {
            return@withContext DevicePollResult.Expired
        }

        try {
            val jsonPayload = JSONObject().apply {
                put("device_auth_id", session.deviceAuthId)
                put("user_code", session.userCode)
            }.toString()

            val requestBody = jsonPayload.toRequestBody(JSON_MEDIA_TYPE)
            val request = Request.Builder()
                .url(TOKEN_ENDPOINT)
                .post(requestBody)
                .addHeader("Accept", "application/json")
                .addHeader("User-Agent", "Codex-CLI/0.1.0 (Android; Mobile)")
                .build()

            val response = httpClient.newCall(request).execute()
            val body = response.body?.string().orEmpty()

            if (response.isSuccessful && body.isNotBlank()) {
                val json = JSONObject(body)
                val accessToken = json.optString("access_token").takeIf { it.isNotEmpty() }
                    ?: json.optString("token").takeIf { it.isNotEmpty() }
                    ?: json.optString("session_token").takeIf { it.isNotEmpty() }
                    ?: session.deviceAuthId

                val refreshToken = json.optString("refresh_token").takeIf { it.isNotEmpty() }
                val idToken = json.optString("id_token").takeIf { it.isNotEmpty() }
                val expiresIn = json.optLong("expires_in", 86400L)
                val decoded = JwtTokenParser.parseToken(idToken ?: accessToken)

                return@withContext DevicePollResult.Success(
                    OAuthTokenResult(
                        accessToken = accessToken,
                        refreshToken = refreshToken,
                        idToken = idToken,
                        expiresInSeconds = expiresIn,
                        decodedInfo = decoded
                    )
                )
            }

            if (response.code == 403 || response.code == 400 || response.code == 404) {
                // Authorization pending or user has not yet submitted in browser
                return@withContext DevicePollResult.Pending
            }

            return@withContext DevicePollResult.Pending
        } catch (e: Exception) {
            return@withContext DevicePollResult.Pending
        }
    }

    fun openBrowser(context: Context, url: String = VERIFICATION_URL) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback
        }
    }

    fun copyToClipboard(context: Context, text: String, label: String = "Codex Auth") {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
    }
}
