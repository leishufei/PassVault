package com.example.passvault.ui.auth

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.passvault.domain.auth.AuthManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authManager: AuthManager,
    private val syncScheduler: com.example.passvault.domain.sync.SyncScheduler
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        checkAuthStatus()
    }

    private fun checkAuthStatus() {
        val isMasterPasswordSet = authManager.isMasterPasswordSet()
        val isBiometricAvailable = authManager.isBiometricAvailable()
        val isBiometricEnabled = authManager.isBiometricEnabled()

        _uiState.value = _uiState.value.copy(
            isSetupRequired = !isMasterPasswordSet,
            isBiometricAvailable = isBiometricAvailable,
            isBiometricEnabled = isBiometricEnabled
        )
    }

    fun setMasterPassword(password: String, confirmPassword: String) {
        viewModelScope.launch {
            if (password != confirmPassword) {
                _uiState.value = _uiState.value.copy(
                    error = "Passwords do not match"
                )
                return@launch
            }

            authManager.setMasterPassword(password)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isAuthenticated = true,
                        error = null
                    )
                    triggerAutoSync()
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        error = e.message
                    )
                }
        }
    }

    fun verifyMasterPassword(password: String) {
        viewModelScope.launch {
            val isValid = authManager.verifyMasterPassword(password)
            if (isValid) {
                _uiState.value = _uiState.value.copy(
                    isAuthenticated = true,
                    error = null
                )
                triggerAutoSync()
            } else {
                _uiState.value = _uiState.value.copy(
                    error = "Invalid password"
                )
            }
        }
    }

    fun onBiometricSuccess() {
        _uiState.value = _uiState.value.copy(
            isAuthenticated = true,
            error = null
        )
        triggerAutoSync()
    }

    private fun triggerAutoSync() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(syncStatus = SyncStatus.Syncing)
                val result = syncScheduler.syncImmediately()
                _uiState.value = _uiState.value.copy(
                    syncStatus = if (result.isSuccess) SyncStatus.Success else SyncStatus.Error
                )
                kotlinx.coroutines.delay(2000)
                _uiState.value = _uiState.value.copy(syncStatus = SyncStatus.Idle)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(syncStatus = SyncStatus.Error)
                kotlinx.coroutines.delay(3000)
                _uiState.value = _uiState.value.copy(syncStatus = SyncStatus.Idle)
            }
        }
    }

    fun onBiometricError(error: String) {
        _uiState.value = _uiState.value.copy(
            error = error
        )
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun authenticateWithBiometric(activity: FragmentActivity) {
        authManager.authenticateWithBiometric(
            activity,
            onSuccess = { onBiometricSuccess() },
            onError = { onBiometricError(it) }
        )
    }

    fun logout() {
        authManager.clearSession()
        _uiState.value = _uiState.value.copy(
            isAuthenticated = false,
            error = null
        )
    }
}

data class AuthUiState(
    val isSetupRequired: Boolean = false,
    val isAuthenticated: Boolean = false,
    val isBiometricAvailable: Boolean = false,
    val isBiometricEnabled: Boolean = false,
    val error: String? = null,
    val syncStatus: SyncStatus = SyncStatus.Idle
)

enum class SyncStatus {
    Idle,
    Syncing,
    Success,
    Error
}
