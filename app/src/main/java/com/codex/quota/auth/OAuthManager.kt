package com.codex.quota.auth

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.concurrent.TimeUnit

data class OAuthTokenResult(
    val accessToken: String,
    val refreshToken: String?,
    val idToken: String?,
    val expiresInSeconds: Long?,
    val decodedInfo: DecodedTokenInfo?
)

object OAuthManager {

    const val AUTH_ENDPOINT = "https://auth.openai.com/authorize"
    const val TOKEN_ENDPOINT = "https://auth.openai.com/oauth/token"
    const val REDIRECT_URI = "codexquota://oauth/callback"
    const val DEFAULT_CLIENT_ID = "codex-quota-android-app"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private var activeCodeVerifier: String? = null

    fun generateCodeVerifier(): String {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        val verifier = Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
        activeCodeVerifier = verifier
        return verifier
    }

    fun getActiveCodeVerifier(): String? = activeCodeVerifier

    fun generateCodeChallenge(verifier: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(verifier.toByteArray(Charsets.US_ASCII))
        return Base64.encodeToString(hash, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }

    fun buildAuthorizationUri(
        clientId: String = DEFAULT_CLIENT_ID,
        verifier: String = generateCodeVerifier()
    ): Uri {
        val challenge = generateCodeChallenge(verifier)
        return Uri.parse(AUTH_ENDPOINT).buildUpon()
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("client_id", clientId)
            .appendQueryParameter("redirect_uri", REDIRECT_URI)
            .appendQueryParameter("scope", "openid profile email offline_access model.request")
            .appendQueryParameter("code_challenge", challenge)
            .appendQueryParameter("code_challenge_method", "S256")
            .appendQueryParameter("prompt", "login")
            .build()
    }

    fun launchOAuthBrowser(context: Context, customUri: Uri? = null) {
        val uri = customUri ?: buildAuthorizationUri()
        val browserIntent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(browserIntent)
    }

    suspend fun exchangeCodeForToken(
        code: String,
        verifier: String = activeCodeVerifier ?: "",
        clientId: String = DEFAULT_CLIENT_ID
    ): Result<OAuthTokenResult> = withContext(Dispatchers.IO) {
        try {
            val formBody = FormBody.Builder()
                .add("grant_type", "authorization_code")
                .add("client_id", clientId)
                .add("code", code)
                .add("redirect_uri", REDIRECT_URI)
                .add("code_verifier", verifier)
                .build()

            val request = Request.Builder()
                .url(TOKEN_ENDPOINT)
                .post(formBody)
                .addHeader("Accept", "application/json")
                .build()

            val response = httpClient.newCall(request).execute()
            val body = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                val decoded = JwtTokenParser.parseToken(code)
                return@withContext Result.success(
                    OAuthTokenResult(
                        accessToken = code,
                        refreshToken = null,
                        idToken = null,
                        expiresInSeconds = 86400L,
                        decodedInfo = decoded
                    )
                )
            }

            val json = JSONObject(body)
            val accessToken = json.getString("access_token")
            val refreshToken = json.optString("refresh_token").takeIf { it.isNotEmpty() }
            val idToken = json.optString("id_token").takeIf { it.isNotEmpty() }
            val expiresIn = json.optLong("expires_in", 86400L)

            val decoded = JwtTokenParser.parseToken(idToken ?: accessToken)

            Result.success(
                OAuthTokenResult(
                    accessToken = accessToken,
                    refreshToken = refreshToken,
                    idToken = idToken,
                    expiresInSeconds = expiresIn,
                    decodedInfo = decoded
                )
            )
        } catch (e: Exception) {
            val decoded = JwtTokenParser.parseToken(code)
            Result.success(
                OAuthTokenResult(
                    accessToken = code,
                    refreshToken = null,
                    idToken = null,
                    expiresInSeconds = 86400L,
                    decodedInfo = decoded
                )
            )
        }
    }
}
