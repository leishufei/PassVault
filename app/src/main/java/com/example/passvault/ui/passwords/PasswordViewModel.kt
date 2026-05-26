package com.example.passvault.ui.passwords

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.passvault.data.model.Category
import com.example.passvault.data.repository.CategoryRepository
import com.example.passvault.data.repository.PasswordEntryDisplay
import com.example.passvault.data.repository.PasswordRepository
import com.example.passvault.domain.generator.PasswordConfig
import com.example.passvault.domain.generator.PasswordGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class PasswordFilter {
    object ALL : PasswordFilter()
    object FAVORITES : PasswordFilter()
    object RECENT : PasswordFilter()
    data class CATEGORY(val categoryId: Long) : PasswordFilter()
}

@HiltViewModel
class PasswordViewModel @Inject constructor(
    private val repository: PasswordRepository,
    private val categoryRepository: CategoryRepository,
    private val passwordGenerator: PasswordGenerator
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filterMode = MutableStateFlow<PasswordFilter>(PasswordFilter.ALL)
    val filterMode: StateFlow<PasswordFilter> = _filterMode.asStateFlow()

    private val _rawPasswords = MutableStateFlow<List<PasswordEntryDisplay>>(emptyList())

    val categories: StateFlow<List<Category>> = categoryRepository.getAllCategories()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val passwords: StateFlow<List<PasswordEntryDisplay>> = combine(
        _rawPasswords,
        _searchQuery,
        _filterMode
    ) { passwords, query, filter ->
        var filtered = passwords

        if (query.isNotEmpty()) {
            filtered = filtered.filter {
                it.title.contains(query, ignoreCase = true) ||
                it.username.contains(query, ignoreCase = true) ||
                it.website.contains(query, ignoreCase = true)
            }
        }

        when (filter) {
            PasswordFilter.ALL -> filtered
            PasswordFilter.FAVORITES -> filtered.filter { it.isFavorite }
            PasswordFilter.RECENT -> filtered.sortedByDescending { it.lastUsedAt }
            is PasswordFilter.CATEGORY -> filtered.filter { it.categoryId == filter.categoryId }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        loadPasswords()
    }

    private fun loadPasswords() {
        viewModelScope.launch {
            repository.getAllPasswords().collect { passwords ->
                _rawPasswords.value = passwords
            }
        }
    }

    fun searchPasswords(query: String) {
        _searchQuery.value = query
    }

    fun setFilter(filter: PasswordFilter) {
        _filterMode.value = filter
    }

    fun toggleFavorite(id: Long, isFavorite: Boolean) {
        viewModelScope.launch {
            repository.updateFavoriteStatus(id, isFavorite)
        }
    }

    fun markAsUsed(id: Long) {
        viewModelScope.launch {
            repository.updateLastUsed(id)
        }
    }

    fun savePassword(
        id: Long?,
        title: String,
        username: String,
        password: String,
        website: String,
        notes: String,
        iconColor: Int = 0,
        strength: Int = 0,
        categoryId: Long = 0
    ) {
        viewModelScope.launch {
            if (id == null) {
                repository.insertPassword(title, username, password, website, notes, iconColor, strength, categoryId)
            } else {
                repository.updatePassword(id, title, username, password, website, notes, iconColor = iconColor, strength = strength, categoryId = categoryId)
            }
        }
    }

    fun deletePassword(id: Long) {
        viewModelScope.launch {
            repository.deletePassword(id)
        }
    }

    fun generatePassword(config: PasswordConfig): String {
        return passwordGenerator.generatePassword(config)
    }
}
