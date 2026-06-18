package com.example.passvault.ui.passwords

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.passvault.data.model.Category
import com.example.passvault.data.repository.CategoryRepository
import com.example.passvault.data.repository.PasswordEntryDisplay
import com.example.passvault.data.repository.PasswordEntryListItem
import com.example.passvault.data.repository.PasswordRepository
import com.example.passvault.data.security.MasterPasswordManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import com.example.passvault.domain.generator.PasswordConfig
import com.example.passvault.domain.generator.PasswordGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class PasswordFilter {
    object FAVORITES : PasswordFilter()
    object RECENT : PasswordFilter()
    data class CATEGORY(val categoryId: Long) : PasswordFilter()
}

enum class SortOrder {
    CREATED_DESC,
    CREATED_ASC
}

@HiltViewModel
class PasswordViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: PasswordRepository,
    private val categoryRepository: CategoryRepository,
    private val passwordGenerator: PasswordGenerator,
    private val syncScheduler: com.example.passvault.domain.sync.SyncScheduler,
    private val masterPasswordManager: MasterPasswordManager
) : ViewModel() {

    private val prefs = context.getSharedPreferences("passvault_filter", Context.MODE_PRIVATE)

    private var lastSelectedCategoryId: Long = prefs.getLong(KEY_SELECTED_CATEGORY, 0L)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filterMode = MutableStateFlow<PasswordFilter>(PasswordFilter.CATEGORY(lastSelectedCategoryId))
    val filterMode: StateFlow<PasswordFilter> = _filterMode.asStateFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.CREATED_DESC)
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    private val _selectionMode = MutableStateFlow(false)
    val selectionMode: StateFlow<Boolean> = _selectionMode.asStateFlow()

    private val _selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedIds: StateFlow<Set<Long>> = _selectedIds.asStateFlow()

    private val _rawPasswords = MutableStateFlow<List<PasswordEntryListItem>>(emptyList())

    val categories: StateFlow<List<Category>> = categoryRepository.getAllCategories()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val categoryCounts: StateFlow<Map<Long, Int>> = _rawPasswords.map { passwords ->
        val counts = mutableMapOf<Long, Int>()
        counts[0L] = passwords.size // 0L represents ALL
        passwords.forEach { pw ->
            counts[pw.categoryId] = (counts[pw.categoryId] ?: 0) + 1
        }
        counts
    }.distinctUntilChanged().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyMap()
    )

    val passwords: StateFlow<List<PasswordEntryListItem>> = combine(
        _rawPasswords,
        _searchQuery,
        _filterMode,
        _sortOrder
    ) { passwords, query, filter, sort ->
        var filtered = passwords

        if (query.isNotEmpty()) {
            filtered = filtered.filter {
                it.title.contains(query, ignoreCase = true) ||
                it.username.contains(query, ignoreCase = true) ||
                it.website.contains(query, ignoreCase = true)
            }
        }

        filtered = when (filter) {
            PasswordFilter.FAVORITES -> filtered.filter { it.isFavorite }
            PasswordFilter.RECENT -> filtered.sortedByDescending { it.lastUsedAt }
            is PasswordFilter.CATEGORY -> {
                if (filter.categoryId == 0L) filtered
                else filtered.filter { it.categoryId == filter.categoryId }
            }
        }

        if (filter != PasswordFilter.RECENT) {
            when (sort) {
                SortOrder.CREATED_DESC -> filtered.sortedByDescending { it.createdAt }
                SortOrder.CREATED_ASC -> filtered.sortedBy { it.createdAt }
            }
        } else {
            filtered
        }
    }
    .distinctUntilChanged()
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        loadPasswords()
    }

    fun setFilter(filter: PasswordFilter) {
        _filterMode.value = filter
        if (filter is PasswordFilter.CATEGORY) {
            lastSelectedCategoryId = filter.categoryId
            prefs.edit().putLong(KEY_SELECTED_CATEGORY, filter.categoryId).apply()
        }
    }

    fun clearTagFilter() {
        val target = PasswordFilter.CATEGORY(lastSelectedCategoryId)
        if (_filterMode.value != target) {
            _filterMode.value = target
        }
    }

    private fun loadPasswords() {
        viewModelScope.launch {
            repository.getAllPasswordsForList().collect { passwords ->
                _rawPasswords.value = passwords
            }
        }
    }

    fun decryptPassword(id: Long, onResult: (String) -> Unit) {
        viewModelScope.launch {
            val password = repository.decryptPassword(id)
            if (password != null) {
                onResult(password)
            }
        }
    }

    suspend fun getPasswordDetail(id: Long): PasswordEntryDisplay? {
        return repository.getPasswordById(id)
    }

    fun searchPasswords(query: String) {
        _searchQuery.value = query
    }

    fun setSortOrder(order: SortOrder) {
        _sortOrder.value = order
    }

    fun toggleFavorite(id: Long, isFavorite: Boolean) {
        viewModelScope.launch {
            repository.updateFavoriteStatus(id, isFavorite)
            triggerAutoSync()
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
        categoryId: Long = 0,
        onDuplicate: (() -> Unit)? = null,
        onSaved: (() -> Unit)? = null
    ) {
        viewModelScope.launch {
            val excludeId = id ?: 0L
            if (repository.checkDuplicate(title, username, excludeId)) {
                onDuplicate?.invoke()
                return@launch
            }
            if (id == null) {
                repository.insertPassword(title, username, password, website, notes, iconColor, strength, categoryId)
            } else {
                repository.updatePassword(id, title, username, password, website, notes, iconColor = iconColor, strength = strength, categoryId = categoryId)
                repository.updateLastUsed(id)
            }
            triggerAutoSync()
            onSaved?.invoke()
        }
    }

    fun deletePassword(id: Long) {
        viewModelScope.launch {
            repository.deletePassword(id)
            triggerAutoSync()
        }
    }

    fun enterSelectionMode(initialId: Long) {
        _selectionMode.value = true
        _selectedIds.value = setOf(initialId)
    }

    fun toggleSelection(id: Long) {
        val cur = _selectedIds.value
        val next = if (id in cur) cur - id else cur + id
        _selectedIds.value = next
        if (next.isEmpty()) _selectionMode.value = false
    }

    fun toggleSelectAll() {
        val visibleIds = passwords.value.map { it.id }.toSet()
        _selectedIds.value = if (_selectedIds.value.containsAll(visibleIds) && visibleIds.isNotEmpty()) {
            emptySet()
        } else {
            visibleIds
        }
    }

    fun exitSelectionMode() {
        _selectionMode.value = false
        _selectedIds.value = emptySet()
    }

    fun bulkDelete() {
        val ids = _selectedIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            repository.bulkDelete(ids)
            triggerAutoSync()
            exitSelectionMode()
        }
    }

    fun bulkChangeCategory(newCategoryId: Long) {
        val ids = _selectedIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            repository.bulkChangeCategory(ids, newCategoryId)
            triggerAutoSync()
            exitSelectionMode()
        }
    }

    fun changeCategory(id: Long, newCategoryId: Long) {
        viewModelScope.launch {
            repository.changeCategory(id, newCategoryId)
            triggerAutoSync()
        }
    }

    private fun triggerAutoSync() {
        syncScheduler.markDirty()
    }

    fun generatePassword(config: PasswordConfig): String {
        return passwordGenerator.generatePassword(config)
    }

    fun getPasswordDefaultVisible(): Boolean {
        return masterPasswordManager.getPasswordDefaultVisible()
    }

    fun addCategory(name: String, colorIndex: Int) {
        viewModelScope.launch {
            val sortOrder = categories.value.size
            categoryRepository.insertCategory(name, colorIndex, sortOrder)
        }
    }

    companion object {
        private const val KEY_SELECTED_CATEGORY = "selected_category_id"
    }
}
