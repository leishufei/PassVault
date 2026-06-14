package com.example.passvault.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.passvault.data.model.Category
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Query("SELECT * FROM categories WHERE isDeleted = 0 ORDER BY sortOrder ASC, createdAt ASC")
    fun getAllCategories(): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getCategoryById(id: Long): Category?

    @Query("SELECT * FROM categories WHERE uuid = :uuid")
    suspend fun getCategoryByUuid(uuid: String): Category?

    @Query("SELECT * FROM categories")
    suspend fun getAllCategoriesIncludingDeleted(): List<Category>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: Category): Long

    @Update
    suspend fun updateCategory(category: Category)

    @Delete
    suspend fun deleteCategory(category: Category)

    @Query("DELETE FROM categories WHERE isDefault = 0")
    suspend fun deleteAllCustomCategories()

    @Query("SELECT COUNT(*) FROM categories WHERE isDeleted = 0")
    suspend fun getCategoryCount(): Int

    @Query("UPDATE categories SET isDeleted = 1, updatedAt = :timestamp WHERE id = :id")
    suspend fun logicalDeleteCategory(id: Long, timestamp: Long)

    @Query("DELETE FROM categories WHERE isDeleted = 1 AND updatedAt < :cutoff")
    suspend fun deleteOldTombstones(cutoff: Long)
}
