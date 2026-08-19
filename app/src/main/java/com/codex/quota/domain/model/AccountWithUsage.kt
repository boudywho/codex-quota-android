package com.codex.quota.domain.model

data class AccountWithUsage(
    val account: CodexAccount,
    val usage: CodexUsage?
)
