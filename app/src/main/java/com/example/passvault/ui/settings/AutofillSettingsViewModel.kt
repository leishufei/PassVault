package com.example.passvault.ui.settings

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.view.autofill.AutofillManager
import androidx.annotation.RequiresApi
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.passvault.domain.auth.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AutofillSettingsUiState(
    val serviceEnabled: Boolean = false,
    val sessionMinutes: Int = 5,
    val inlineSuggestionsEnabled: Boolean = true
)

@HiltViewModel
class AutofillSettingsViewModel @Inject constructor(
    application: Application,
    private val sessionManager: SessionManager
) : AndroidViewModel(application) {

    private val prefs: SharedPreferences = application.getSharedPreferences(
        PREFS_NAME, Context.MODE_PRIVATE
    )

    private val _uiState = MutableStateFlow(loadInitialState())
    val uiState: StateFlow<AutofillSettingsUiState> = _uiState.asStateFlow()

    fun refreshServiceState() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(serviceEnabled = isAutofillServiceEnabled())
        }
    }

    fun setSessionMinutes(minutes: Int) {
        prefs.edit().putInt(KEY_SESSION_MINUTES, minutes).apply()
        _uiState.value = _uiState.value.copy(sessionMinutes = minutes)
    }

    fun setInlineSuggestionsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_INLINE_ENABLED, enabled).apply()
        _uiState.value = _uiState.value.copy(inlineSuggestionsEnabled = enabled)
    }

    fun clearAutofillSession() {
        sessionManager.clearAutofillSession()
    }

    fun disableAutofillService() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val context = getApplication<Application>().applicationContext
        val manager = context.getSystemService(AutofillManager::class.java) ?: return
        if (manager.hasEnabledAutofillServices()) {
            manager.disableAutofillServices()
        }
        refreshServiceState()
    }

    private fun loadInitialState(): AutofillSettingsUiState {
        return AutofillSettingsUiState(
            serviceEnabled = isAutofillServiceEnabled(),
            sessionMinutes = prefs.getInt(KEY_SESSION_MINUTES, 5),
            inlineSuggestionsEnabled = prefs.getBoolean(KEY_INLINE_ENABLED, true)
        )
    }

    private fun isAutofillServiceEnabled(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
        val context = getApplication<Application>().applicationContext
        val manager = context.getSystemService(AutofillManager::class.java) ?: return false
        return manager.hasEnabledAutofillServices()
    }

    companion object {
        private const val PREFS_NAME = "autofill_prefs"
        private const val KEY_SESSION_MINUTES = "session_minutes"
        private const val KEY_INLINE_ENABLED = "inline_enabled"
    }
}
