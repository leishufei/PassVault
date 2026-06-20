package com.example.passvault.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.passvault.data.model.Category
import com.example.passvault.data.repository.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CategoryViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    val categories: StateFlow<List<Category>> = categoryRepository.getAllCategories()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        repairCategorySortOrderIfNeeded()
    }

    private fun repairCategorySortOrderIfNeeded() {
        viewModelScope.launch {
            val categoriesList = categoryRepository.getAllCategories().first()
            val zeroSortOrderCount = categoriesList.count { it.sortOrder == 0 }
            if (zeroSortOrderCount > 1) {
                categoriesList.forEachIndexed { index, category ->
                    categoryRepository.updateCategorySortOrder(category.id, index)
                }
            }
        }
    }

    fun addCategory(name: String, color: Int) {
        viewModelScope.launch {
            val sortOrder = categories.value.size
            categoryRepository.insertCategory(name, color, sortOrder)
        }
    }

    fun updateCategory(id: Long, name: String, color: Int) {
        viewModelScope.launch {
            categoryRepository.updateCategory(id, name, color)
        }
    }

    fun deleteCategory(id: Long) {
        viewModelScope.launch {
            categoryRepository.deleteCategory(id)
        }
    }
}
