package com.example.passvault.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "password_entries")
data class PasswordEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val uuid: String = UUID.randomUUID().toString(),
    val title: String,
    val username: String,
    val encryptedPassword: ByteArray,
    val passwordIv: ByteArray,
    val website: String = "",
    val notes: String = "",
    val isFavorite: Boolean = false,
    val lastUsedAt: Long = 0L,
    val iconColor: Int = 0,
    val strength: Int = 0,
    val categoryId: Long = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as PasswordEntry
        if (id != other.id) return false
        if (title != other.title) return false
        if (username != other.username) return false
        if (!encryptedPassword.contentEquals(other.encryptedPassword)) return false
        if (!passwordIv.contentEquals(other.passwordIv)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + username.hashCode()
        result = 31 * result + encryptedPassword.contentHashCode()
        result = 31 * result + passwordIv.contentHashCode()
        return result
    }
}
