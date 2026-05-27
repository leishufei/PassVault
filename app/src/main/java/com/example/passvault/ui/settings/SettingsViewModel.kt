package com.example.passvault.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.passvault.data.repository.CategoryRepository
import com.example.passvault.data.repository.PasswordRepository
import com.example.passvault.data.repository.PasswordStatistics
import com.example.passvault.data.security.MasterPasswordManager
import com.example.passvault.domain.auth.AuthManager
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authManager: AuthManager,
    private val passwordRepository: PasswordRepository,
    private val masterPasswordManager: MasterPasswordManager,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _shouldLockNow = MutableStateFlow(false)
    val shouldLockNow: StateFlow<Boolean> = _shouldLockNow.asStateFlow()

    init {
        loadSettings()
        loadPasswordStatistics()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            val biometricEnabled = authManager.isBiometricEnabled()
            val autoLockTime = masterPasswordManager.getAutoLockTime()
            _uiState.value = _uiState.value.copy(
                biometricEnabled = biometricEnabled,
                autoLockTime = autoLockTime
            )
        }
    }

    private fun loadPasswordStatistics() {
        viewModelScope.launch {
            try {
                val stats = passwordRepository.getPasswordStatistics()
                _uiState.value = _uiState.value.copy(passwordStats = stats)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(passwordStats = null)
            }
        }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        authManager.setBiometricEnabled(enabled)
        _uiState.value = _uiState.value.copy(biometricEnabled = enabled)
    }

    fun setAutoLockTime(time: String) {
        masterPasswordManager.setAutoLockTime(time)
        _uiState.value = _uiState.value.copy(autoLockTime = time)

        // 如果设置为"立即"，触发锁定
        if (time == "立即") {
            _shouldLockNow.value = true
        }
    }

    fun resetLockNowFlag() {
        _shouldLockNow.value = false
    }

    fun changeMasterPassword(oldPassword: String, newPassword: String): Result<Unit> {
        return try {
            if (!authManager.verifyMasterPassword(oldPassword)) {
                Result.failure(Exception("旧密码不正确"))
            } else if (newPassword.length < 8) {
                Result.failure(Exception("新密码至少需要8个字符"))
            } else {
                authManager.setMasterPassword(newPassword)
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun exportCsv(uri: Uri): Result<Unit> {
        return try {
            viewModelScope.launch {
                val passwords = passwordRepository.getAllPasswords().first()
                val categories = categoryRepository.getAllCategories().first()
                val categoryMap = categories.associate { it.id to it.name }

                val rows = buildList {
                    add(listOf("分类", "名称", "用户名", "密码"))
                    passwords.forEach { password ->
                        val categoryName = categoryMap[password.categoryId] ?: "未分类"
                        add(listOf(categoryName, password.title, password.username, password.password))
                    }
                }

                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    csvWriter().open(outputStream) {
                        writeRows(rows)
                    }
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun importCsv(uri: Uri): Result<Unit> {
        return try {
            viewModelScope.launch {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val records = csvReader().readAll(inputStream)
                    if (records.isEmpty()) return@launch

                    val startIdx = if (records[0].size >= 4 &&
                        (records[0][0].contains("分类") || records[0][1].contains("名称"))) 1 else 0

                    val currentCategories = categoryRepository.getAllCategories().first().toMutableList()
                    val passwordsToInsert = mutableListOf<com.example.passvault.data.repository.PasswordImportData>()

                    for (idx in startIdx until records.size) {
                        val record = records[idx]
                        if (record.size < 4) continue
                        val categoryName = record[0].trim()
                        val title = record[1].trim()
                        val username = record[2].trim()
                        val password = record[3]

                        if (title.isEmpty()) continue

                        var categoryId = 0L
                        if (categoryName.isNotEmpty() && categoryName != "未分类") {
                            val existingCategory = currentCategories.find { it.name.equals(categoryName, ignoreCase = true) }
                            if (existingCategory != null) {
                                categoryId = existingCategory.id
                            } else {
                                val newColor = (0..5).random()
                                val newId = categoryRepository.insertCategory(categoryName, newColor)
                                categoryId = newId
                                currentCategories.add(
                                    com.example.passvault.data.model.Category(
                                        id = newId,
                                        name = categoryName,
                                        color = newColor
                                    )
                                )
                            }
                        }

                        passwordsToInsert.add(
                            com.example.passvault.data.repository.PasswordImportData(
                                title = title,
                                username = username,
                                password = password,
                                categoryId = categoryId
                            )
                        )
                    }
                    
                    if (passwordsToInsert.isNotEmpty()) {
                        passwordRepository.insertPasswords(passwordsToInsert)
                    }
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

data class SettingsUiState(
    val biometricEnabled: Boolean = false,
    val autoLockTime: String = "5分钟",
    val passwordStats: PasswordStatistics? = null
)
