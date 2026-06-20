package com.example.passvault.data.repository

import com.example.passvault.data.local.CategoryDao
import com.example.passvault.data.local.PasswordDao
import com.example.passvault.data.model.Category
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepository @Inject constructor(
    private val categoryDao: CategoryDao,
    private val passwordDao: PasswordDao
) {

    fun getAllCategories(): Flow<List<Category>> {
        return categoryDao.getAllCategories()
    }

    suspend fun getCategoryById(id: Long): Category? {
        return categoryDao.getCategoryById(id)
    }

    suspend fun insertCategory(name: String, color: Int, sortOrder: Int = 0): Long {
        val category = Category(
            name = name,
            color = color,
            isDefault = false,
            sortOrder = sortOrder
        )
        return categoryDao.insertCategory(category)
    }

    suspend fun updateCategory(id: Long, name: String, color: Int) {
        val category = categoryDao.getCategoryById(id)
        category?.let {
            val updated = it.copy(name = name, color = color)
            categoryDao.updateCategory(updated)
        }
    }

    suspend fun deleteCategory(id: Long) {
        val category = categoryDao.getCategoryById(id)
        category?.let {
            if (!it.isDefault) {
                passwordDao.updatePasswordsCategory(id, 0)
                categoryDao.logicalDeleteCategory(id, System.currentTimeMillis())
            }
        }
    }

    suspend fun updateCategorySortOrder(id: Long, sortOrder: Int) {
        val category = categoryDao.getCategoryById(id)
        category?.let {
            val updated = it.copy(sortOrder = sortOrder)
            categoryDao.updateCategory(updated)
        }
    }

    suspend fun initializeDefaultCategories() {
        val allCategories = categoryDao.getAllCategories().first()
        val defaultCategories = allCategories.filter { it.isDefault }
        defaultCategories.forEach { category ->
            passwordDao.updatePasswordsCategory(category.id, 0)
            categoryDao.deleteCategory(category)
        }
    }
}
