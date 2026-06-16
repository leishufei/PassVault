package com.example.passvault.data.repository

import com.example.passvault.data.local.PasswordDao
import com.example.passvault.data.model.PasswordEntry
import com.example.passvault.data.security.CryptoManager
import com.example.passvault.data.security.EncryptedData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PasswordRepository @Inject constructor(
    private val passwordDao: PasswordDao,
    private val cryptoManager: CryptoManager
) {

    fun getAllPasswords(): Flow<List<PasswordEntryDisplay>> {
        return passwordDao.getAllPasswords().map { entries ->
            entries.map { it.toDisplay() }
        }
    }

    fun getAllPasswordsForList(): Flow<List<PasswordEntryListItem>> {
        return passwordDao.getAllPasswords().map { entries ->
            entries.map { it.toListItem() }
        }
    }

    suspend fun decryptPassword(id: Long): String? {
        val entry = passwordDao.getPasswordById(id) ?: return null
        return cryptoManager.decrypt(EncryptedData(entry.encryptedPassword, entry.passwordIv))
    }

    suspend fun getPasswordById(id: Long): PasswordEntryDisplay? {
        return passwordDao.getPasswordById(id)?.toDisplay()
    }

    fun searchPasswords(query: String): Flow<List<PasswordEntryDisplay>> {
        return passwordDao.searchPasswords(query).map { entries ->
            entries.map { it.toDisplay() }
        }
    }

    suspend fun insertPassword(
        title: String,
        username: String,
        password: String,
        website: String = "",
        notes: String = "",
        iconColor: Int = 0,
        strength: Int = 0,
        categoryId: Long = 0
    ): Long {
        val encryptedData = cryptoManager.encrypt(password)
        val entry = PasswordEntry(
            title = title,
            username = username,
            encryptedPassword = encryptedData.ciphertext,
            passwordIv = encryptedData.iv,
            website = website,
            notes = notes,
            iconColor = iconColor,
            strength = strength,
            categoryId = categoryId
        )
        return passwordDao.insertPassword(entry)
    }

    suspend fun insertPasswords(passwords: List<PasswordImportData>) {
        val entries = passwords.map { data ->
            val encryptedData = cryptoManager.encrypt(data.password)
            PasswordEntry(
                title = data.title,
                username = data.username,
                encryptedPassword = encryptedData.ciphertext,
                passwordIv = encryptedData.iv,
                website = data.website,
                notes = data.notes,
                iconColor = data.iconColor,
                strength = data.strength,
                categoryId = data.categoryId
            )
        }
        passwordDao.insertPasswords(entries)
    }

    suspend fun updatePassword(
        id: Long,
        title: String,
        username: String,
        password: String,
        website: String = "",
        notes: String = "",
        isFavorite: Boolean = false,
        iconColor: Int = 0,
        strength: Int = 0,
        categoryId: Long = 0
    ) {
        val existing = passwordDao.getPasswordById(id)
        val encryptedData = cryptoManager.encrypt(password)
        val entry = PasswordEntry(
            id = id,
            uuid = existing?.uuid ?: java.util.UUID.randomUUID().toString(),
            title = title,
            username = username,
            encryptedPassword = encryptedData.ciphertext,
            passwordIv = encryptedData.iv,
            website = website,
            notes = notes,
            isFavorite = isFavorite,
            iconColor = iconColor,
            strength = strength,
            categoryId = categoryId,
            createdAt = existing?.createdAt ?: System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        passwordDao.updatePassword(entry)
    }

    suspend fun updateFavoriteStatus(id: Long, isFavorite: Boolean) {
        passwordDao.updateFavoriteStatus(id, isFavorite)
    }

    suspend fun updateLastUsed(id: Long) {
        passwordDao.updateLastUsed(id, System.currentTimeMillis())
    }

    suspend fun deletePassword(id: Long) {
        passwordDao.logicalDelete(id, System.currentTimeMillis())
    }

    fun getPasswordsByCategory(categoryId: Long): Flow<List<PasswordEntryDisplay>> {
        return passwordDao.getPasswordsByCategory(categoryId).map { entries ->
            entries.map { it.toDisplay() }
        }
    }

    suspend fun checkDuplicate(title: String, username: String, excludeId: Long = 0L): Boolean {
        return passwordDao.countDuplicate(title, username, excludeId) > 0
    }

    private fun PasswordEntry.toDisplay(): PasswordEntryDisplay {
        val decryptedPassword = cryptoManager.decrypt(
            EncryptedData(encryptedPassword, passwordIv)
        )
        return PasswordEntryDisplay(
            id = id,
            title = title,
            username = username,
            password = decryptedPassword,
            website = website,
            notes = notes,
            isFavorite = isFavorite,
            lastUsedAt = lastUsedAt,
            iconColor = iconColor,
            strength = strength,
            categoryId = categoryId,
            createdAt = createdAt,
            updatedAt = updatedAt,
            isDeleted = isDeleted
        )
    }

    private fun PasswordEntry.toListItem(): PasswordEntryListItem {
        return PasswordEntryListItem(
            id = id,
            title = title,
            username = username,
            website = website,
            isFavorite = isFavorite,
            lastUsedAt = lastUsedAt,
            iconColor = iconColor,
            strength = strength,
            categoryId = categoryId,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    suspend fun getPasswordStatistics(): PasswordStatistics {
        val entries = passwordDao.getAllPasswordsForAnalysis()
        if (entries.isEmpty()) {
            return PasswordStatistics(0, 0, 0, 0, 0)
        }

        val passwords = entries.map { entry ->
            cryptoManager.decrypt(EncryptedData(entry.encryptedPassword, entry.passwordIv))
        }

        var strongCount = 0
        var weakCount = 0
        val passwordCounts = mutableMapOf<String, Int>()

        passwords.forEach { password ->
            val strength = calculatePasswordStrength(password)
            when (strength) {
                3 -> strongCount++
                1 -> weakCount++
            }
            passwordCounts[password] = (passwordCounts[password] ?: 0) + 1
        }

        val duplicateCount = passwordCounts.count { it.value > 1 }
        val mediumCount = entries.size - strongCount - weakCount

        val strongRatio = strongCount.toFloat() / entries.size
        val duplicateRatio = if (duplicateCount > 0) duplicateCount.toFloat() / entries.size else 0f
        val score = ((strongRatio * 0.6f + (1 - duplicateRatio) * 0.4f) * 100).toInt()

        return PasswordStatistics(
            totalCount = entries.size,
            strongCount = strongCount,
            weakCount = weakCount,
            duplicateCount = duplicateCount,
            averageStrength = score
        )
    }

    private fun calculatePasswordStrength(password: String): Int {
        val length = password.length
        val hasUppercase = password.any { it.isUpperCase() }
        val hasLowercase = password.any { it.isLowerCase() }
        val hasDigit = password.any { it.isDigit() }
        val hasSpecial = password.any { !it.isLetterOrDigit() }

        val charTypeCount = listOf(hasUppercase, hasLowercase, hasDigit, hasSpecial).count { it }

        return when {
            length >= 12 && charTypeCount >= 3 -> 3
            length >= 8 && charTypeCount >= 2 -> 2
            else -> 1
        }
    }
}

data class PasswordEntryDisplay(
    val id: Long,
    val title: String,
    val username: String,
    val password: String,
    val website: String,
    val notes: String,
    val isFavorite: Boolean,
    val lastUsedAt: Long,
    val iconColor: Int,
    val strength: Int,
    val categoryId: Long,
    val createdAt: Long,
    val updatedAt: Long,
    val isDeleted: Boolean
)

data class PasswordEntryListItem(
    val id: Long,
    val title: String,
    val username: String,
    val website: String,
    val isFavorite: Boolean,
    val lastUsedAt: Long,
    val iconColor: Int,
    val strength: Int,
    val categoryId: Long,
    val createdAt: Long,
    val updatedAt: Long
)

data class PasswordStatistics(
    val totalCount: Int,
    val strongCount: Int,
    val weakCount: Int,
    val duplicateCount: Int,
    val averageStrength: Int
)

data class PasswordImportData(
    val title: String,
    val username: String,
    val password: String,
    val website: String = "",
    val notes: String = "",
    val iconColor: Int = 0,
    val strength: Int = 0,
    val categoryId: Long = 0
)
