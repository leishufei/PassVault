package com.example.passwordmanager.data.repository

import com.example.passwordmanager.data.local.PasswordDao
import com.example.passwordmanager.data.model.PasswordEntry
import com.example.passwordmanager.data.security.CryptoManager
import com.example.passwordmanager.data.security.EncryptedData
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
        strength: Int = 0
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
            strength = strength
        )
        return passwordDao.insertPassword(entry)
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
        strength: Int = 0
    ) {
        val encryptedData = cryptoManager.encrypt(password)
        val entry = PasswordEntry(
            id = id,
            title = title,
            username = username,
            encryptedPassword = encryptedData.ciphertext,
            passwordIv = encryptedData.iv,
            website = website,
            notes = notes,
            isFavorite = isFavorite,
            iconColor = iconColor,
            strength = strength,
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
        val entry = passwordDao.getPasswordById(id)
        entry?.let { passwordDao.deletePassword(it) }
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
            createdAt = createdAt,
            updatedAt = updatedAt
        )
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
    val createdAt: Long,
    val updatedAt: Long
)
