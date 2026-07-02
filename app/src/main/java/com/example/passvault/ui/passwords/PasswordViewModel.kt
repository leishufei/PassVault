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
import com.example.passvault.ui.theme.CategoryConstants
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

    private val _isSearchActive = MutableStateFlow(false)
    val isSearchActive: StateFlow<Boolean> = _isSearchActive.asStateFlow()

    private val _selectedCategoryId = MutableStateFlow(lastSelectedCategoryId)
    val selectedCategoryId: StateFlow<Long> = _selectedCategoryId.asStateFlow()

    private val _isFavoritesOnly = MutableStateFlow(false)
    val isFavoritesOnly: StateFlow<Boolean> = _isFavoritesOnly.asStateFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.CREATED_DESC)
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    private val _selectionMode = MutableStateFlow(false)
    val selectionMode: StateFlow<Boolean> = _selectionMode.asStateFlow()

    private val _selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedIds: StateFlow<Set<Long>> = _selectedIds.asStateFlow()

    private val _rawPasswords = MutableStateFlow<List<PasswordEntryListItem>>(emptyList())

    val categories: StateFlow<List<Category>> = categoryRepository.getAllCategories()
        .map { dbCategories ->
            val uncategorized = Category(
                id = CategoryConstants.UNCATEGORIZED_ID,
                name = CategoryConstants.UNCATEGORIZED_NAME,
                color = -1,
                isDefault = true,
                sortOrder = -1
            )
            listOf(uncategorized) + dbCategories
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val searchAndFavoritesFiltered: StateFlow<List<PasswordEntryListItem>> = combine(
        _rawPasswords,
        _searchQuery,
        _isFavoritesOnly
    ) { passwords, query, favoritesOnly ->
        var filtered = passwords
        if (query.isNotEmpty()) {
            filtered = filtered.filter {
                it.title.contains(query, ignoreCase = true) ||
                it.username.contains(query, ignoreCase = true) ||
                it.website.contains(query, ignoreCase = true)
            }
        }
        if (favoritesOnly) {
            filtered = filtered.filter { it.isFavorite }
        }
        filtered
    }
    .distinctUntilChanged()
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val categoryCounts: StateFlow<Map<Long, Int>> = searchAndFavoritesFiltered.map { passwords ->
        val counts = mutableMapOf<Long, Int>()
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
        searchAndFavoritesFiltered,
        _selectedCategoryId,
        _sortOrder
    ) { passwords, categoryId, sort ->
        val filtered = passwords.filter { it.categoryId == categoryId }
        when (sort) {
            SortOrder.CREATED_DESC -> filtered.sortedByDescending { it.createdAt }
            SortOrder.CREATED_ASC -> filtered.sortedBy { it.createdAt }
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
        viewModelScope.launch {
            categoryRepository.dedupByName()
        }
    }

    fun setCategory(categoryId: Long) {
        _selectedCategoryId.value = categoryId
        lastSelectedCategoryId = categoryId
        prefs.edit().putLong(KEY_SELECTED_CATEGORY, categoryId).apply()
    }

    fun toggleFavorites() {
        _isFavoritesOnly.value = !_isFavoritesOnly.value
    }

    fun reorderCategories(orderedIds: List<Long>) {
        viewModelScope.launch {
            categoryRepository.updateCategorySortOrders(orderedIds)
            triggerAutoSync()
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

    fun setSearchActive(active: Boolean) {
        _isSearchActive.value = active
        if (!active) {
            _searchQuery.value = ""
        }
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

    fun copyToCategory(id: Long, newCategoryId: Long) {
        viewModelScope.launch {
            repository.duplicateToCategory(id, newCategoryId)
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

    fun deleteCategory(categoryId: Long) {
        if (categoryId == CategoryConstants.UNCATEGORIZED_ID || categoryId == 0L) return
        viewModelScope.launch {
            categoryRepository.deleteCategory(categoryId)
            if (_selectedCategoryId.value == categoryId) {
                setCategory(0L)
            }
            triggerAutoSync()
        }
    }

    companion object {
        private const val KEY_SELECTED_CATEGORY = "selected_category_id"
    }
}
