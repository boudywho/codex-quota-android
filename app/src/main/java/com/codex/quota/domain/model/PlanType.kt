package com.codex.quota.domain.model

enum class PlanType(val displayName: String) {
    PLUS("ChatGPT Plus / Codex"),
    TEAM("OpenAI Team"),
    ENTERPRISE("OpenAI Enterprise"),
    API_TIER_1("API Usage Tier 1"),
    API_TIER_2("API Usage Tier 2"),
    API_TIER_5("API Usage Tier 5 / Custom"),
    MOCK_DEMO("Demo Simulation");

    companion object {
        fun fromString(value: String): PlanType {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: PLUS
        }
    }
}
