package com.example.passwordmanager.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.passwordmanager.data.model.PasswordEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface PasswordDao {

    @Query("SELECT * FROM password_entries ORDER BY updatedAt DESC")
    fun getAllPasswords(): Flow<List<PasswordEntry>>

    @Query("SELECT * FROM password_entries WHERE id = :id")
    suspend fun getPasswordById(id: Long): PasswordEntry?

    @Query("SELECT * FROM password_entries WHERE title LIKE '%' || :query || '%' OR username LIKE '%' || :query || '%' OR website LIKE '%' || :query || '%'")
    fun searchPasswords(query: String): Flow<List<PasswordEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPassword(entry: PasswordEntry): Long

    @Update
    suspend fun updatePassword(entry: PasswordEntry)

    @Query("UPDATE password_entries SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavoriteStatus(id: Long, isFavorite: Boolean)

    @Query("UPDATE password_entries SET lastUsedAt = :timestamp WHERE id = :id")
    suspend fun updateLastUsed(id: Long, timestamp: Long)

    @Delete
    suspend fun deletePassword(entry: PasswordEntry)

    @Query("DELETE FROM password_entries")
    suspend fun deleteAllPasswords()

    @Query("SELECT * FROM password_entries")
    suspend fun getAllPasswordsForAnalysis(): List<PasswordEntry>
}
