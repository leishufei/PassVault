package com.example.passvault.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.passvault.data.model.PasswordEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface PasswordDao {

    @Query("SELECT * FROM password_entries WHERE isDeleted = 0 ORDER BY updatedAt DESC")
    fun getAllPasswords(): Flow<List<PasswordEntry>>

    @Query("SELECT * FROM password_entries WHERE id = :id")
    suspend fun getPasswordById(id: Long): PasswordEntry?

    @Query("SELECT * FROM password_entries WHERE uuid = :uuid")
    suspend fun getPasswordByUuid(uuid: String): PasswordEntry?

    @Query("SELECT * FROM password_entries WHERE isDeleted = 0 AND (title LIKE '%' || :query || '%' OR username LIKE '%' || :query || '%' OR website LIKE '%' || :query || '%')")
    fun searchPasswords(query: String): Flow<List<PasswordEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPassword(entry: PasswordEntry): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPasswords(entries: List<PasswordEntry>)

    @Update
    suspend fun updatePassword(entry: PasswordEntry)

    @Query("UPDATE password_entries SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavoriteStatus(id: Long, isFavorite: Boolean)

    @Query("UPDATE password_entries SET lastUsedAt = :timestamp WHERE id = :id")
    suspend fun updateLastUsed(id: Long, timestamp: Long)

    @Delete
    suspend fun deletePassword(entry: PasswordEntry)

    @Query("UPDATE password_entries SET isDeleted = 1, updatedAt = :timestamp WHERE id = :id")
    suspend fun logicalDelete(id: Long, timestamp: Long)

    @Query("DELETE FROM password_entries")
    suspend fun deleteAllPasswords()

    @Query("SELECT * FROM password_entries")
    fun getAllPasswordsForSync(): Flow<List<PasswordEntry>>

    @Query("SELECT * FROM password_entries")
    suspend fun getAllPasswordsIncludingDeleted(): List<PasswordEntry>

    @Query("SELECT * FROM password_entries WHERE isDeleted = 0")
    suspend fun getAllPasswordsForAnalysis(): List<PasswordEntry>

    @Query("SELECT * FROM password_entries WHERE categoryId = :categoryId AND isDeleted = 0 ORDER BY updatedAt DESC")
    fun getPasswordsByCategory(categoryId: Long): Flow<List<PasswordEntry>>

    @Query("SELECT COUNT(*) FROM password_entries WHERE title = :title AND username = :username AND isDeleted = 0 AND id != :excludeId")
    suspend fun countDuplicate(title: String, username: String, excludeId: Long): Int

    @Query("UPDATE password_entries SET categoryId = :newCategoryId WHERE categoryId = :oldCategoryId")
    suspend fun updatePasswordsCategory(oldCategoryId: Long, newCategoryId: Long)

    @Query("DELETE FROM password_entries WHERE isDeleted = 1 AND updatedAt < :cutoff")
    suspend fun deleteOldTombstones(cutoff: Long)
}
