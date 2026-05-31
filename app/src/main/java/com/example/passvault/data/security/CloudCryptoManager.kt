package com.example.passvault.data.security

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

data class CloudBackupFormat(
    val version: Int = 1,
    val kdfAlgorithm: String = "PBKDF2WithHmacSHA256",
    val iterations: Int = 600000,
    val salt: String, // Base64
    val encryptionAlgorithm: String = "AES-256-GCM",
    val iv: String, // Base64
    val ciphertext: String // Base64
)

@Singleton
class CloudCryptoManager @Inject constructor() {

    companion object {
        private const val KDF_ALGORITHM = "PBKDF2WithHmacSHA256"
        private const val ITERATIONS = 600000
        private const val KEY_LENGTH_BITS = 256
        private const val SALT_LENGTH_BYTES = 16
        private const val IV_LENGTH_BYTES = 12
        private const val GCM_TAG_LENGTH_BITS = 128
        private const val ENCRYPTION_ALGORITHM = "AES/GCM/NoPadding"
    }

    fun encryptForCloud(plaintext: String, masterPassword: String): CloudBackupFormat {
        // 1. Generate random salt
        val salt = ByteArray(SALT_LENGTH_BYTES)
        SecureRandom().nextBytes(salt)

        // 2. Derive key
        val key = deriveKey(masterPassword, salt, ITERATIONS)

        // 3. Generate random IV
        val iv = ByteArray(IV_LENGTH_BYTES)
        SecureRandom().nextBytes(iv)

        // 4. Encrypt
        val cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.ENCRYPT_MODE, key, spec)
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))

        // 5. Return formatted data
        return CloudBackupFormat(
            salt = Base64.encodeToString(salt, Base64.NO_WRAP),
            iv = Base64.encodeToString(iv, Base64.NO_WRAP),
            ciphertext = Base64.encodeToString(ciphertext, Base64.NO_WRAP)
        )
    }

    fun decryptFromCloud(backup: CloudBackupFormat, masterPassword: String): String {
        // Validate supported algorithms
        require(backup.kdfAlgorithm == KDF_ALGORITHM) { "Unsupported KDF Algorithm" }
        require(backup.encryptionAlgorithm == "AES-256-GCM") { "Unsupported Encryption Algorithm" }

        val salt = Base64.decode(backup.salt, Base64.DEFAULT)
        val iv = Base64.decode(backup.iv, Base64.DEFAULT)
        val ciphertext = Base64.decode(backup.ciphertext, Base64.DEFAULT)

        val key = deriveKey(masterPassword, salt, backup.iterations)

        val cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, spec)
        val plaintext = cipher.doFinal(ciphertext)

        return String(plaintext, Charsets.UTF_8)
    }

    private fun deriveKey(password: String, salt: ByteArray, iterations: Int): SecretKeySpec {
        val factory = SecretKeyFactory.getInstance(KDF_ALGORITHM)
        val spec = PBEKeySpec(password.toCharArray(), salt, iterations, KEY_LENGTH_BITS)
        val secret = factory.generateSecret(spec)
        return SecretKeySpec(secret.encoded, "AES")
    }
}
