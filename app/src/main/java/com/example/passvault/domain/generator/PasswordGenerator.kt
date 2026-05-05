package com.example.passvault.domain.generator

import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PasswordGenerator @Inject constructor() {

    private val secureRandom = SecureRandom()

    fun generatePassword(config: PasswordConfig): String {
        val charPool = buildCharPool(config)
        if (charPool.isEmpty()) {
            throw IllegalArgumentException("At least one character type must be selected")
        }

        val password = StringBuilder()
        repeat(config.length) {
            val randomIndex = secureRandom.nextInt(charPool.length)
            password.append(charPool[randomIndex])
        }

        return password.toString()
    }

    private fun buildCharPool(config: PasswordConfig): String {
        return buildString {
            if (config.includeUppercase) append(UPPERCASE)
            if (config.includeLowercase) append(LOWERCASE)
            if (config.includeNumbers) append(NUMBERS)
            if (config.includeSymbols) append(SYMBOLS)
        }
    }

    companion object {
        private const val UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        private const val LOWERCASE = "abcdefghijklmnopqrstuvwxyz"
        private const val NUMBERS = "0123456789"
        private const val SYMBOLS = "!@#$%^&*()_+-=[]{}|;:,.<>?"
    }
}

data class PasswordConfig(
    val length: Int = 16,
    val includeUppercase: Boolean = true,
    val includeLowercase: Boolean = true,
    val includeNumbers: Boolean = true,
    val includeSymbols: Boolean = true
)
