package com.example.passvault.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.passvault.data.repository.CategoryRepository
import com.example.passvault.data.repository.PasswordImportData
import com.example.passvault.data.repository.PasswordRepository
import com.example.passvault.data.repository.PasswordStatistics
import com.example.passvault.data.security.MasterPasswordManager
import com.example.passvault.domain.auth.AuthManager
import com.example.passvault.domain.csv.CsvValidator
import com.example.passvault.domain.csv.CsvValidationResult
import com.example.passvault.domain.csv.ImportAction
import com.example.passvault.domain.csv.ImportProgress
import com.example.passvault.domain.csv.ValidatedPasswordRow
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: android.content.Context,
    private val authManager: AuthManager,
    private val passwordRepository: PasswordRepository,
    private val masterPasswordManager: MasterPasswordManager,
    private val categoryRepository: CategoryRepository,
    private val syncManager: com.example.passvault.domain.sync.SyncManager,
    private val syncScheduler: com.example.passvault.domain.sync.SyncScheduler,
    private val googleDriveProvider: com.example.passvault.data.sync.GoogleDriveProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _shouldLockNow = MutableStateFlow(false)
    val shouldLockNow: StateFlow<Boolean> = _shouldLockNow.asStateFlow()

    private val _importProgress = MutableStateFlow<ImportProgress>(ImportProgress.Idle)
    val importProgress: StateFlow<ImportProgress> = _importProgress.asStateFlow()

    private val csvValidator = CsvValidator()

    init {
        loadSettings()
        trySilentRestore()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            val biometricEnabled = authManager.isBiometricEnabled()
            val autoLockTime = masterPasswordManager.getAutoLockTime()
            val passwordDefaultVisible = masterPasswordManager.getPasswordDefaultVisible()
            _uiState.value = _uiState.value.copy(
                biometricEnabled = biometricEnabled,
                autoLockTime = autoLockTime,
                passwordDefaultVisible = passwordDefaultVisible
            )
        }
    }

    /** App 重启后尝试静默恢复 Google Drive 登录状态 */
    private fun trySilentRestore() {
        viewModelScope.launch {
            val result = googleDriveProvider.trySilentSignIn()
            if (result.isSuccess) {
                syncManager.currentProvider = googleDriveProvider
                _uiState.value = _uiState.value.copy(currentProvider = "Google Drive")
            }
        }
    }

    fun loadPasswordStatisticsIfNeeded() {
        if (_uiState.value.passwordStats != null) return
        viewModelScope.launch {
            try {
                val stats = withContext(Dispatchers.IO) {
                    passwordRepository.getPasswordStatistics()
                }
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

    fun setPasswordDefaultVisible(visible: Boolean) {
        masterPasswordManager.setPasswordDefaultVisible(visible)
        _uiState.value = _uiState.value.copy(passwordDefaultVisible = visible)
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
                    add(CsvValidator.EXTENDED_HEADERS)
                    passwords.forEach { password ->
                        val categoryName = categoryMap[password.categoryId] ?: CsvValidator.UNCATEGORIZED
                        add(
                            listOf(
                                password.uuid,
                                categoryName,
                                password.title,
                                password.username,
                                password.password,
                                password.website,
                                password.notes
                            )
                        )
                    }
                }

                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        outputStream.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
                        csvWriter().open(outputStream) {
                            writeRows(rows)
                        }
                    }
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Reads and validates a CSV file. The result is published via [importProgress];
     * callers observe the flow and show the preview dialog when the state becomes
     * [ImportProgress.ValidationComplete].
     */
    fun validateCsv(uri: Uri) {
        viewModelScope.launch {
            _importProgress.value = ImportProgress.Validating
            try {
                val result = withContext(Dispatchers.IO) {
                    val records = context.contentResolver.openInputStream(uri)?.use { input ->
                        csvReader().readAll(input)
                    } ?: emptyList()
                    val passwords = passwordRepository.getAllPasswords().first()
                    val categories = categoryRepository.getAllCategories().first()
                    csvValidator.validate(records, passwords, categories)
                }
                _importProgress.value = ImportProgress.ValidationComplete(result)
            } catch (e: Exception) {
                _importProgress.value = ImportProgress.Error("解析失败: ${e.message}")
            }
        }
    }

    /**
     * Executes the import based on a previously computed [CsvValidationResult].
     * Handles category creation, batched updates, and inserts in IO context.
     */
    fun executeImport(result: CsvValidationResult) {
        viewModelScope.launch {
            try {
                val total = result.validRows.size
                if (total == 0) {
                    _importProgress.value = ImportProgress.Complete(0, 0)
                    return@launch
                }

                val categoryIdByName = withContext(Dispatchers.IO) {
                    val categories = categoryRepository.getAllCategories().first().toMutableList()
                    val map = categories.associate { it.name.lowercase() to it.id }.toMutableMap()
                    result.newCategoryNames.forEach { name ->
                        val key = name.lowercase()
                        if (!map.containsKey(key)) {
                            val newId = categoryRepository.insertCategory(name, (0..5).random())
                            map[key] = newId
                        }
                    }
                    map
                }

                var updated = 0
                var inserted = 0
                val pendingInserts = mutableListOf<PasswordImportData>()

                withContext(Dispatchers.IO) {
                    result.validRows.forEachIndexed { index, row ->
                        _importProgress.value = ImportProgress.Importing(index, total)
                        val categoryId = resolveCategoryId(row, categoryIdByName)
                        when (row.action) {
                            ImportAction.UPDATE -> {
                                val ok = passwordRepository.updatePasswordFromImport(
                                    id = row.existingId ?: 0L,
                                    title = row.title,
                                    username = row.username,
                                    password = row.password,
                                    website = row.website,
                                    notes = row.notes,
                                    categoryId = categoryId
                                )
                                if (ok) updated++ else pendingInserts.add(row.toImportData(categoryId))
                            }
                            ImportAction.INSERT -> {
                                pendingInserts.add(row.toImportData(categoryId))
                            }
                        }
                    }
                    if (pendingInserts.isNotEmpty()) {
                        passwordRepository.insertPasswords(pendingInserts)
                        inserted += pendingInserts.size
                    }
                }

                _importProgress.value = ImportProgress.Complete(updated, inserted)
            } catch (e: Exception) {
                _importProgress.value = ImportProgress.Error("导入失败: ${e.message}")
            }
        }
    }

    fun resetImportProgress() {
        _importProgress.value = ImportProgress.Idle
    }

    private fun resolveCategoryId(
        row: ValidatedPasswordRow,
        categoryIdByName: Map<String, Long>
    ): Long {
        if (row.categoryName == CsvValidator.UNCATEGORIZED) return 0L
        return categoryIdByName[row.categoryName.lowercase()] ?: 0L
    }

    private fun ValidatedPasswordRow.toImportData(categoryId: Long): PasswordImportData {
        return PasswordImportData(
            title = title,
            username = username,
            password = password,
            website = website,
            notes = notes,
            categoryId = categoryId
        )
    }

    /** 返回 Google 登录 Intent，由 UI 层通过 ActivityResultLauncher 发起 */
    fun getGoogleSignInIntent(): Intent {
        return googleDriveProvider.getSignInIntent()
    }

    /** 处理 Google 登录结果回调 */
    fun handleGoogleSignInResult(data: Intent?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSyncing = true, lastSyncMessage = "正在连接 Google Drive...")
            val result = googleDriveProvider.handleSignInResult(data)
            if (result.isSuccess) {
                syncManager.currentProvider = googleDriveProvider
                _uiState.value = _uiState.value.copy(
                    currentProvider = "Google Drive",
                    lastSyncMessage = "已绑定 Google Drive",
                    isSyncing = false
                )
            } else {
                syncManager.currentProvider = null
                _uiState.value = _uiState.value.copy(
                    isSyncing = false,
                    lastSyncMessage = "绑定失败: ${result.exceptionOrNull()?.message}"
                )
            }
        }
    }

    fun syncNow() {
        if (syncManager.currentProvider == null) {
            _uiState.value = _uiState.value.copy(lastSyncMessage = "请先绑定云端账号")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSyncing = true, lastSyncMessage = "正在同步中...")
            val result = withContext(Dispatchers.IO) {
                syncScheduler.syncImmediately()
            }
            _uiState.value = _uiState.value.copy(
                isSyncing = false,
                lastSyncMessage = if (result.isSuccess) "同步成功" else "同步失败: ${result.exceptionOrNull()?.message}"
            )
        }
    }
}

data class SettingsUiState(
    val biometricEnabled: Boolean = false,
    val autoLockTime: String = "5分钟",
    val passwordDefaultVisible: Boolean = false,
    val passwordStats: PasswordStatistics? = null,
    val isSyncing: Boolean = false,
    val lastSyncMessage: String? = null,
    val currentProvider: String? = null
)
