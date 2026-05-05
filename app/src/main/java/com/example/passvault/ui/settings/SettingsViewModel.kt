package com.example.passvault.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.passvault.data.repository.PasswordRepository
import com.example.passvault.data.repository.PasswordStatistics
import com.example.passvault.data.security.MasterPasswordManager
import com.example.passvault.domain.auth.AuthManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authManager: AuthManager,
    private val passwordRepository: PasswordRepository,
    private val masterPasswordManager: MasterPasswordManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

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

    fun exportBackup(uri: Uri): Result<Unit> {
        return try {
            viewModelScope.launch {
                val passwords = passwordRepository.getAllPasswords().first()
                val jsonArray = JSONArray()

                passwords.forEach { password ->
                    val jsonObject = JSONObject().apply {
                        put("title", password.title)
                        put("username", password.username)
                        put("password", password.password)
                        put("website", password.website)
                        put("notes", password.notes)
                        put("isFavorite", password.isFavorite)
                        put("createdAt", password.createdAt)
                    }
                    jsonArray.put(jsonObject)
                }

                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(jsonArray.toString(2).toByteArray())
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun importBackup(uri: Uri): Result<Unit> {
        return try {
            viewModelScope.launch {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val jsonString = inputStream.bufferedReader().use { it.readText() }
                    val jsonArray = JSONArray(jsonString)

                    for (i in 0 until jsonArray.length()) {
                        val jsonObject = jsonArray.getJSONObject(i)
                        passwordRepository.insertPassword(
                            title = jsonObject.getString("title"),
                            username = jsonObject.getString("username"),
                            password = jsonObject.getString("password"),
                            website = jsonObject.optString("website", ""),
                            notes = jsonObject.optString("notes", "")
                        )
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
