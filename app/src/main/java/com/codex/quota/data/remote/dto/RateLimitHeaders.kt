package com.codex.quota.data.remote.dto

import okhttp3.Headers
import java.util.regex.Pattern

data class ParsedRateLimits(
    val limitRequests: Long?,
    val remainingRequests: Long?,
    val resetRequests: String?,
    val resetRequestsMs: Long?,
    val limitTokens: Long?,
    val remainingTokens: Long?,
    val resetTokens: String?,
    val resetTokensMs: Long?
) {
    companion object {
        fun fromHeaders(headers: Headers): ParsedRateLimits {
            val limitReq = headers["x-ratelimit-limit-requests"]?.toLongOrNull()
            val remReq = headers["x-ratelimit-remaining-requests"]?.toLongOrNull()
            val resetReq = headers["x-ratelimit-reset-requests"]
            val resetReqMs = parseDurationToMillis(resetReq)

            val limitTok = headers["x-ratelimit-limit-tokens"]?.toLongOrNull()
            val remTok = headers["x-ratelimit-remaining-tokens"]?.toLongOrNull()
            val resetTok = headers["x-ratelimit-reset-tokens"]
            val resetTokMs = parseDurationToMillis(resetTok)

            return ParsedRateLimits(
                limitRequests = limitReq,
                remainingRequests = remReq,
                resetRequests = resetReq,
                resetRequestsMs = resetReqMs,
                limitTokens = limitTok,
                remainingTokens = remTok,
                resetTokens = resetTok,
                resetTokensMs = resetTokMs
            )
        }

        fun parseDurationToMillis(durationStr: String?): Long? {
            if (durationStr.isNullOrBlank()) return null
            try {
                var totalMs = 0L
                val matcher = Pattern.compile("(\\d+(\\.\\d+)?)(ms|s|m|h|d)").matcher(durationStr.trim().lowercase())
                var found = false
                while (matcher.find()) {
                    found = true
                    val value = matcher.group(1)?.toDoubleOrNull() ?: 0.0
                    val unit = matcher.group(3)
                    val ms = when (unit) {
                        "ms" -> value
                        "s" -> value * 1000.0
                        "m" -> value * 60.0 * 1000.0
                        "h" -> value * 60.0 * 60.0 * 1000.0
                        "d" -> value * 24.0 * 60.0 * 60.0 * 1000.0
                        else -> 0.0
                    }
                    totalMs += ms.toLong()
                }
                return if (found) totalMs else durationStr.toLongOrNull()
            } catch (e: Exception) {
                return null
            }
        }
    }
}
