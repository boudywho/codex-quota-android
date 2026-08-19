package com.codex.quota.data.remote

import com.codex.quota.domain.model.CodexAccount
import com.codex.quota.domain.model.CodexUsage

interface CodexAccountDataSource {
    suspend fun fetchUsage(
        account: CodexAccount,
        apiKey: String
    ): Result<CodexUsage>
}
