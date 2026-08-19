package com.codex.quota.data.remote.dto

import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

data class ChatGptAccountCheckData(
    val accountCreatedEpochMs: Long?,
    val subscriptionRenewsEpochMs: Long?,
    val subscriptionExpiresEpochMs: Long?,
    val billingPeriod: String?,
    val willRenew: Boolean?,
    val hasActiveSubscription: Boolean?,
    val planType: String?
) {
    companion object {
        fun fromJson(jsonStr: String, targetAccountId: String?): ChatGptAccountCheckData? {
            return try {
                val root = JSONObject(jsonStr)
                val accountsObj = root.optJSONObject("accounts") ?: root

                var accountItem: JSONObject? = null
                if (targetAccountId != null && accountsObj.has(targetAccountId)) {
                    accountItem = accountsObj.optJSONObject(targetAccountId)
                }

                if (accountItem == null && accountsObj.has("default")) {
                    accountItem = accountsObj.optJSONObject("default")
                }

                if (accountItem == null && accountsObj.length() > 0) {
                    val firstKey = accountsObj.keys().next()
                    accountItem = accountsObj.optJSONObject(firstKey)
                }

                if (accountItem == null) return null

                val accountObj = accountItem.optJSONObject("account")
                val entitlementObj = accountItem.optJSONObject("entitlement")
                val lastSubObj = accountItem.optJSONObject("last_active_subscription")

                val createdTimeStr = accountObj?.optString("created_time", "")
                val accountCreatedEpochMs = if (!createdTimeStr.isNullOrEmpty()) parseIsoDate(createdTimeStr) else null

                val renewsAtStr = entitlementObj?.optString("renews_at", "")
                val subscriptionRenewsEpochMs = if (!renewsAtStr.isNullOrEmpty()) parseIsoDate(renewsAtStr) else null

                val expiresAtStr = entitlementObj?.optString("expires_at", "")
                val subscriptionExpiresEpochMs = if (!expiresAtStr.isNullOrEmpty()) parseIsoDate(expiresAtStr) else null

                val billingPeriod = entitlementObj?.optString("billing_period")?.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString()
                }

                val hasActiveSubscription = entitlementObj?.optBoolean("has_active_subscription", false)
                val willRenew = lastSubObj?.optBoolean("will_renew", false)
                val planType = accountObj?.optString("plan_type")

                ChatGptAccountCheckData(
                    accountCreatedEpochMs = accountCreatedEpochMs,
                    subscriptionRenewsEpochMs = subscriptionRenewsEpochMs ?: subscriptionExpiresEpochMs,
                    subscriptionExpiresEpochMs = subscriptionExpiresEpochMs,
                    billingPeriod = billingPeriod,
                    willRenew = willRenew,
                    hasActiveSubscription = hasActiveSubscription,
                    planType = planType
                )
            } catch (e: Exception) {
                null
            }
        }

        private fun parseIsoDate(isoDateStr: String): Long? {
            return try {
                val cleanStr = isoDateStr.substringBefore('.').substringBefore('+').substringBefore('Z')
                val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                sdf.parse(cleanStr)?.time
            } catch (e: Exception) {
                null
            }
        }
    }
}
