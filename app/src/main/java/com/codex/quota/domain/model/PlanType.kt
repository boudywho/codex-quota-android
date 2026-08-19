package com.codex.quota.domain.model

enum class PlanType(val displayName: String) {
    PLUS("ChatGPT Plus"),
    TEAM("ChatGPT Team"),
    ENTERPRISE("ChatGPT Enterprise"),
    API_TIER_1("OpenAI Tier 1"),
    API_TIER_2("OpenAI Tier 2"),
    API_TIER_5("OpenAI Tier 5"),
    MOCK_DEMO("Demo Account");

    companion object {
        fun fromString(value: String): PlanType {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: PLUS
        }
    }
}
