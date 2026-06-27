package com.example.passvault.domain.auth

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor() {
    // Keeps the master password in memory while the app is unlocked.
    // Must be cleared when the user logs out.
    var currentMasterPassword: String? = null
        private set

    @Volatile
    private var autofillSessionExpiresAt: Long = 0L

    fun onUnlock(masterPassword: String) {
        currentMasterPassword = masterPassword
    }

    fun onLock() {
        currentMasterPassword = null
        autofillSessionExpiresAt = 0L
    }

    val isUnlocked: Boolean
        get() = currentMasterPassword != null

    fun extendAutofillSession(durationMillis: Long = DEFAULT_AUTOFILL_SESSION_MS) {
        autofillSessionExpiresAt = System.currentTimeMillis() + durationMillis
    }

    fun clearAutofillSession() {
        autofillSessionExpiresAt = 0L
    }

    val isAutofillSessionValid: Boolean
        get() = System.currentTimeMillis() < autofillSessionExpiresAt

    val autofillSessionRemainingMs: Long
        get() = (autofillSessionExpiresAt - System.currentTimeMillis()).coerceAtLeast(0L)

    companion object {
        const val DEFAULT_AUTOFILL_SESSION_MS: Long = 5 * 60 * 1000L
    }
}
