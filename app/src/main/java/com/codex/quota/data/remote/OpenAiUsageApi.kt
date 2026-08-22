package com.codex.quota.data.remote

import com.codex.quota.data.remote.dto.ChatGptAccountCheckData
import com.codex.quota.data.remote.dto.ChatGptWhamUsageDto
import com.codex.quota.data.remote.dto.OpenAiModelsResponseDto
import com.codex.quota.data.remote.dto.ParsedRateLimits
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

sealed class ApiResponse<out T> {
    data class Success<out T>(val data: T, val rateLimits: ParsedRateLimits, val httpCode: Int) : ApiResponse<T>()
    data class HttpError(val httpCode: Int, val message: String, val rateLimits: ParsedRateLimits?) : ApiResponse<Nothing>()
    data class NetworkError(val exception: Throwable) : ApiResponse<Nothing>()
}

interface OpenAiUsageService {
    suspend fun checkAuthenticationAndFetchRateLimits(
        apiKey: String,
        organizationId: String? = null
    ): ApiResponse<OpenAiModelsResponseDto>

    suspend fun fetchChatGptSubscriberUsage(
        accessToken: String,
        chatgptAccountId: String? = null
    ): ApiResponse<ChatGptWhamUsageDto>

    suspend fun fetchChatGptAccountCheck(
        accessToken: String,
        chatgptAccountId: String? = null
    ): ApiResponse<ChatGptAccountCheckData>
}

class OpenAiUsageApi(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()
) : OpenAiUsageService {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    override suspend fun checkAuthenticationAndFetchRateLimits(
        apiKey: String,
        organizationId: String?
    ): ApiResponse<OpenAiModelsResponseDto> = withContext(Dispatchers.IO) {
        val requestBuilder = Request.Builder()
            .url("https://api.openai.com/v1/models")
            .header("Authorization", "Bearer $apiKey")
            .header("User-Agent", "CodexQuota-Android/1.0 (Android; Mobile)")

        if (!organizationId.isNullOrBlank()) {
            requestBuilder.header("OpenAI-Organization", organizationId)
        }

        try {
            val response: Response = client.newCall(requestBuilder.build()).execute()
            val rateLimits = ParsedRateLimits.fromHeaders(response.headers)
            val code = response.code
            val bodyString = response.body?.string().orEmpty()

            if (response.isSuccessful) {
                try {
                    val dto = if (bodyString.isNotBlank()) {
                        json.decodeFromString<OpenAiModelsResponseDto>(bodyString)
                    } else {
                        OpenAiModelsResponseDto()
                    }
                    ApiResponse.Success(dto, rateLimits, code)
                } catch (e: Exception) {
                    ApiResponse.Success(OpenAiModelsResponseDto(), rateLimits, code)
                }
            } else {
                val errorMsg = when (code) {
                    401 -> "Invalid API key or revoked authentication."
                    403 -> "Access forbidden for this organization or project."
                    429 -> "Rate limit or quota threshold reached."
                    in 500..599 -> "OpenAI servers are temporarily unavailable ($code)."
                    else -> "HTTP $code: $bodyString"
                }
                ApiResponse.HttpError(code, errorMsg, rateLimits)
            }
        } catch (e: IOException) {
            ApiResponse.NetworkError(e)
        } catch (e: Exception) {
            ApiResponse.NetworkError(e)
        }
    }

    override suspend fun fetchChatGptSubscriberUsage(
        accessToken: String,
        chatgptAccountId: String?
    ): ApiResponse<ChatGptWhamUsageDto> = withContext(Dispatchers.IO) {
        val cleanToken = accessToken.trim().removePrefix("Bearer ").removePrefix("bearer ")
        val requestBuilder = Request.Builder()
            .url("https://chatgpt.com/backend-api/wham/usage")
            .header("Authorization", "Bearer $cleanToken")
            .header("User-Agent", "CodexQuota-Android/1.0 (Android; Mobile)")
            .header("Accept", "application/json")

        if (!chatgptAccountId.isNullOrBlank()) {
            requestBuilder.header("ChatGPT-Account-ID", chatgptAccountId)
        }

        try {
            val response: Response = client.newCall(requestBuilder.build()).execute()
            val rateLimits = ParsedRateLimits.fromHeaders(response.headers)
            val code = response.code
            val bodyString = response.body?.string().orEmpty()

            if (response.isSuccessful && bodyString.isNotBlank()) {
                try {
                    val dto = json.decodeFromString<ChatGptWhamUsageDto>(bodyString)
                    ApiResponse.Success(dto, rateLimits, code)
                } catch (e: Exception) {
                    ApiResponse.NetworkError(e)
                }
            } else {
                val errorMsg = when (code) {
                    401 -> "ChatGPT subscriber session expired. Please re-authenticate."
                    403 -> "ChatGPT access forbidden."
                    429 -> "Usage rate limit reached."
                    in 500..599 -> "ChatGPT servers temporarily unavailable ($code)."
                    else -> "HTTP $code: $bodyString"
                }
                ApiResponse.HttpError(code, errorMsg, rateLimits)
            }
        } catch (e: IOException) {
            ApiResponse.NetworkError(e)
        } catch (e: Exception) {
            ApiResponse.NetworkError(e)
        }
    }

    override suspend fun fetchChatGptAccountCheck(
        accessToken: String,
        chatgptAccountId: String?
    ): ApiResponse<ChatGptAccountCheckData> = withContext(Dispatchers.IO) {
        val cleanToken = accessToken.trim().removePrefix("Bearer ").removePrefix("bearer ")
        val requestBuilder = Request.Builder()
            .url("https://chatgpt.com/backend-api/accounts/check/v4-2023-04-27")
            .header("Authorization", "Bearer $cleanToken")
            .header("User-Agent", "CodexQuota-Android/1.0 (Android; Mobile)")
            .header("Accept", "application/json")

        if (!chatgptAccountId.isNullOrBlank()) {
            requestBuilder.header("ChatGPT-Account-ID", chatgptAccountId)
        }

        try {
            val response: Response = client.newCall(requestBuilder.build()).execute()
            val rateLimits = ParsedRateLimits.fromHeaders(response.headers)
            val code = response.code
            val bodyString = response.body?.string().orEmpty()

            if (response.isSuccessful && bodyString.isNotBlank()) {
                val data = ChatGptAccountCheckData.fromJson(bodyString, chatgptAccountId)
                if (data != null) {
                    ApiResponse.Success(data, rateLimits, code)
                } else {
                    ApiResponse.HttpError(code, "Failed to parse account details", rateLimits)
                }
            } else {
                ApiResponse.HttpError(code, "Failed to fetch account check ($code)", rateLimits)
            }
        } catch (e: IOException) {
            ApiResponse.NetworkError(e)
        } catch (e: Exception) {
            ApiResponse.NetworkError(e)
        }
    }
}
