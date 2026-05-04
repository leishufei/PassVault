package com.example.passwordmanager.ui.passwords

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.passwordmanager.data.repository.PasswordEntryDisplay
import com.example.passwordmanager.data.repository.PasswordRepository
import com.example.passwordmanager.domain.generator.PasswordConfig
import com.example.passwordmanager.domain.generator.PasswordGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PasswordViewModel @Inject constructor(
    private val repository: PasswordRepository,
    private val passwordGenerator: PasswordGenerator
) : ViewModel() {

    private val _passwords = MutableStateFlow<List<PasswordEntryDisplay>>(emptyList())
    val passwords: StateFlow<List<PasswordEntryDisplay>> = _passwords.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    init {
        loadPasswords()
    }

    private fun loadPasswords() {
        viewModelScope.launch {
            repository.getAllPasswords().collect { passwords ->
                _passwords.value = passwords
            }
        }
    }

    fun searchPasswords(query: String) {
        _searchQuery.value = query
        viewModelScope.launch {
            if (query.isEmpty()) {
                loadPasswords()
            } else {
                repository.searchPasswords(query).collect { passwords ->
                    _passwords.value = passwords
                }
            }
        }
    }

    fun savePassword(
        id: Long?,
        title: String,
        username: String,
        password: String,
        website: String,
        notes: String
    ) {
        viewModelScope.launch {
            if (id == null) {
                repository.insertPassword(title, username, password, website, notes)
            } else {
                repository.updatePassword(id, title, username, password, website, notes)
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
