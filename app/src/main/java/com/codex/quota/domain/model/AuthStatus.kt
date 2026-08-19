package com.codex.quota.domain.model

enum class AuthStatus(val isError: Boolean, val userMessage: String) {
    AUTHENTICATED(false, "Active and authenticated"),
    REFRESHING(false, "Updating quota metrics…"),
    OFFLINE(false, "Offline. Showing cached snapshot."),
    TEMPORARY_ERROR(true, "Temporary sync issue. Will retry shortly."),
    AUTHENTICATION_REQUIRED(true, "Authentication required. Please sign in again."),
    UNKNOWN(false, "Status unknown");

    val isSignedOut: Boolean
        get() = this == AUTHENTICATION_REQUIRED
}
