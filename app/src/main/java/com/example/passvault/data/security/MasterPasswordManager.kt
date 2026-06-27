package com.example.passvault.data.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MasterPasswordManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun isMasterPasswordSet(): Boolean {
        return sharedPreferences.contains(KEY_MASTER_PASSWORD_HASH)
    }

    fun setMasterPassword(password: String) {
        val hash = hashPassword(password)
        sharedPreferences.edit()
            .putString(KEY_MASTER_PASSWORD_HASH, hash)
            .putString(KEY_MASTER_PASSWORD_PLAINTEXT, password) // EncryptedSharedPreferences encrypts this
            .apply()
    }

    fun verifyMasterPassword(password: String): Boolean {
        val storedHash = sharedPreferences.getString(KEY_MASTER_PASSWORD_HASH, null)
            ?: return false
        return storedHash == hashPassword(password)
    }

    fun getDecryptedMasterPassword(): String? {
        return sharedPreferences.getString(KEY_MASTER_PASSWORD_PLAINTEXT, null)
    }

    fun isBiometricEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_BIOMETRIC_ENABLED, false)
    }

    fun setBiometricEnabled(enabled: Boolean) {
        sharedPreferences.edit()
            .putBoolean(KEY_BIOMETRIC_ENABLED, enabled)
            .apply()
    }

    fun getPasswordDefaultVisible(): Boolean {
        return sharedPreferences.getBoolean(KEY_PASSWORD_DEFAULT_VISIBLE, false)
    }

    fun setPasswordDefaultVisible(visible: Boolean) {
        sharedPreferences.edit()
            .putBoolean(KEY_PASSWORD_DEFAULT_VISIBLE, visible)
            .apply()
    }

    private fun hashPassword(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(password.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val PREFS_NAME = "master_password_prefs"
        private const val KEY_MASTER_PASSWORD_HASH = "master_password_hash"
        private const val KEY_MASTER_PASSWORD_PLAINTEXT = "master_password_plaintext"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        private const val KEY_PASSWORD_DEFAULT_VISIBLE = "password_default_visible"
    }
}
