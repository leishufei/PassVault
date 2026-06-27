package com.example.passvault.domain.auth

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.passvault.data.security.MasterPasswordManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val masterPasswordManager: MasterPasswordManager,
    private val sessionManager: SessionManager
) {

    fun isMasterPasswordSet(): Boolean {
        return masterPasswordManager.isMasterPasswordSet()
    }

    fun setMasterPassword(password: String): Result<Unit> {
        return try {
            if (password.length < MIN_PASSWORD_LENGTH) {
                Result.failure(Exception("Password must be at least $MIN_PASSWORD_LENGTH characters"))
            } else {
                masterPasswordManager.setMasterPassword(password)
                sessionManager.onUnlock(password)
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun verifyMasterPassword(password: String): Boolean {
        val isValid = masterPasswordManager.verifyMasterPassword(password)
        if (isValid) {
            sessionManager.onUnlock(password)
        }
        return isValid
    }

    fun isBiometricAvailable(): Boolean {
        val biometricManager = BiometricManager.from(context)
        return biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG
        ) == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun isBiometricEnabled(): Boolean {
        return masterPasswordManager.isBiometricEnabled()
    }

    fun setBiometricEnabled(enabled: Boolean) {
        masterPasswordManager.setBiometricEnabled(enabled)
    }

    fun authenticateWithBiometric(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(context)
        val biometricPrompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    val pwd = masterPasswordManager.getDecryptedMasterPassword()
                    if (pwd != null) {
                        sessionManager.onUnlock(pwd)
                        onSuccess()
                    } else {
                        onError("Failed to retrieve master password from Keystore.")
                    }
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    onError(errString.toString())
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onError("Authentication failed")
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Biometric Authentication")
            .setSubtitle("Authenticate to access your passwords")
            .setNegativeButtonText("Use Master Password")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    fun clearSession() {
        sessionManager.onLock()
    }

    companion object {
        private const val MIN_PASSWORD_LENGTH = 8
    }
}
