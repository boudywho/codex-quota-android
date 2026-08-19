package com.codex.quota.security

import android.content.Context
import android.content.SharedPreferences

interface CredentialStore {
    fun storeApiKey(accountId: String, apiKey: String)
    fun getApiKey(accountId: String): String?
    fun removeApiKey(accountId: String)
    fun clearAll()
}

class EncryptedCredentialStore(
    context: Context,
    private val keystoreManager: KeystoreManager
) : CredentialStore {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    override fun storeApiKey(accountId: String, apiKey: String) {
        val encrypted = keystoreManager.encrypt(apiKey)
        prefs.edit().putString(KEY_PREFIX + accountId, encrypted).apply()
    }

    override fun getApiKey(accountId: String): String? {
        val encrypted = prefs.getString(KEY_PREFIX + accountId, null) ?: return null
        return keystoreManager.decrypt(encrypted)
    }

    override fun removeApiKey(accountId: String) {
        prefs.edit().remove(KEY_PREFIX + accountId).apply()
    }

    override fun clearAll() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val PREFS_NAME = "secure_credentials"
        private const val KEY_PREFIX = "key_acc_"
    }
}
