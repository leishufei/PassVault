package com.example.passwordmanager.ui.passwords

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.passwordmanager.data.repository.PasswordEntryDisplay
import com.example.passwordmanager.data.repository.PasswordRepository
import com.example.passwordmanager.domain.generator.PasswordConfig
import com.example.passwordmanager.domain.generator.PasswordGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class PasswordFilter {
    ALL, FAVORITES, RECENT
}

@HiltViewModel
class PasswordViewModel @Inject constructor(
    private val repository: PasswordRepository,
    private val passwordGenerator: PasswordGenerator
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filterMode = MutableStateFlow(PasswordFilter.ALL)
    val filterMode: StateFlow<PasswordFilter> = _filterMode.asStateFlow()

    private val _rawPasswords = MutableStateFlow<List<PasswordEntryDisplay>>(emptyList())

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
        strength: Int = 0
    ) {
        viewModelScope.launch {
            if (id == null) {
                repository.insertPassword(title, username, password, website, notes, iconColor, strength)
            } else {
                repository.updatePassword(id, title, username, password, website, notes, iconColor = iconColor, strength = strength)
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
