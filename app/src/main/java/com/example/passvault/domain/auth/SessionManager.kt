package com.example.passvault.domain.auth

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor() {
    // Keeps the master password in memory while the app is unlocked.
    // Must be cleared when the app auto-locks or user logs out.
    var currentMasterPassword: String? = null
        private set

    fun onUnlock(masterPassword: String) {
        currentMasterPassword = masterPassword
    }

    fun onLock() {
        currentMasterPassword = null
    }

    val isUnlocked: Boolean
        get() = currentMasterPassword != null
}
