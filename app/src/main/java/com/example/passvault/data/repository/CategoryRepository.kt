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
                val now = System.currentTimeMillis()
                val passwordIds = passwordDao.getPasswordIdsByCategory(id)
                if (passwordIds.isNotEmpty()) {
                    passwordDao.bulkLogicalDelete(passwordIds, now)
                }
                categoryDao.logicalDeleteCategory(id, now)
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

    suspend fun updateCategorySortOrders(orderedIds: List<Long>) {
        val now = System.currentTimeMillis()
        orderedIds.forEachIndexed { index, id ->
            categoryDao.updateSortOrder(id, index, now)
        }
    }

    suspend fun dedupByName() {
        val now = System.currentTimeMillis()
        val all = categoryDao.getAllCategoriesIncludingDeleted()
            .filter { !it.isDeleted && !it.isDefault && it.id != 0L }
        val grouped = all.groupBy { it.name.trim().lowercase() }
        grouped.values.filter { it.size > 1 }.forEach { dups ->
            val winner = dups.minByOrNull { it.createdAt } ?: return@forEach
            val losers = dups.filter { it.id != winner.id }
            losers.forEach { loser ->
                passwordDao.bulkMigrateCategory(loser.id, winner.id, now)
                categoryDao.logicalDeleteCategory(loser.id, now)
            }
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
