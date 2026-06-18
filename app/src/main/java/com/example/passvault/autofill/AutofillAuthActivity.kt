package com.example.passvault.autofill

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.autofill.AutofillId
import android.view.autofill.AutofillManager
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.example.passvault.domain.auth.AuthManager
import com.example.passvault.domain.auth.SessionManager
import com.example.passvault.ui.theme.PassVaultTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
@RequiresApi(Build.VERSION_CODES.O)
class AutofillAuthActivity : FragmentActivity() {

    @Inject lateinit var authManager: AuthManager
    @Inject lateinit var sessionManager: SessionManager
    @Inject lateinit var responseBuilder: AutofillResponseBuilder
    @Inject lateinit var matchingEngine: AutofillMatchingEngine

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Main + job)

    private val errorState = mutableStateOf<String?>(null)
    private val loadingState = mutableStateOf(false)

    private lateinit var autofillCtx: AutofillContext

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        autofillCtx = AutofillContext(
            packageName = intent.getStringExtra(EXTRA_PACKAGE).orEmpty(),
            webDomain = intent.getStringExtra(EXTRA_WEB_DOMAIN),
            usernameFieldId = intent.getParcelableExtraCompat(EXTRA_USERNAME_ID),
            passwordFieldId = intent.getParcelableExtraCompat(EXTRA_PASSWORD_ID),
            usernameValue = null,
            passwordValue = null
        )

        setContent {
            PassVaultTheme {
                AutofillAuthDialog(
                    error = errorState.value,
                    loading = loadingState.value,
                    onSubmit = ::handleSubmit,
                    onBiometric = ::handleBiometric,
                    onCancel = ::cancelAndFinish,
                    biometricEnabled = authManager.isBiometricAvailable() && authManager.isBiometricEnabled()
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    private fun handleSubmit(password: String) {
        if (password.isBlank() || loadingState.value) return
        loadingState.value = true
        errorState.value = null
        scope.launch {
            val ok = withContext(Dispatchers.IO) { authManager.verifyMasterPassword(password) }
            if (!ok) {
                errorState.value = "主密码错误"
                loadingState.value = false
                return@launch
            }
            sessionManager.extendAutofillSession()
            replyWithDatasets()
        }
    }

    private fun handleBiometric() {
        if (loadingState.value) return
        loadingState.value = true
        errorState.value = null
        authManager.authenticateWithBiometric(
            activity = this,
            onSuccess = {
                sessionManager.extendAutofillSession()
                scope.launch { replyWithDatasets() }
            },
            onError = { msg ->
                errorState.value = msg
                loadingState.value = false
            }
        )
    }

    private suspend fun replyWithDatasets() {
        val matches = withContext(Dispatchers.IO) { matchingEngine.findMatches(autofillCtx) }
        val response = withContext(Dispatchers.Default) {
            responseBuilder.buildDatasetsResponse(autofillCtx, matches, null)
        }
        if (response == null) {
            cancelAndFinish()
            return
        }
        val replyIntent = Intent().apply {
            putExtra(AutofillManager.EXTRA_AUTHENTICATION_RESULT, response)
        }
        setResult(Activity.RESULT_OK, replyIntent)
        finish()
    }

    private fun cancelAndFinish() {
        setResult(Activity.RESULT_CANCELED)
        finish()
    }

    companion object {
        private const val EXTRA_PACKAGE = "extra_package"
        private const val EXTRA_WEB_DOMAIN = "extra_web_domain"
        private const val EXTRA_USERNAME_ID = "extra_username_id"
        private const val EXTRA_PASSWORD_ID = "extra_password_id"

        fun newIntent(
            context: Context,
            packageName: String,
            webDomain: String?,
            usernameFieldId: AutofillId?,
            passwordFieldId: AutofillId?
        ): Intent = Intent(context, AutofillAuthActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(EXTRA_PACKAGE, packageName)
            if (webDomain != null) putExtra(EXTRA_WEB_DOMAIN, webDomain)
            if (usernameFieldId != null) putExtra(EXTRA_USERNAME_ID, usernameFieldId)
            if (passwordFieldId != null) putExtra(EXTRA_PASSWORD_ID, passwordFieldId)
        }
    }
}

@Suppress("DEPRECATION")
private inline fun <reified T : android.os.Parcelable> Intent.getParcelableExtraCompat(name: String): T? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableExtra(name, T::class.java)
    } else {
        getParcelableExtra(name) as? T
    }
}

@Composable
private fun AutofillAuthDialog(
    error: String?,
    loading: Boolean,
    onSubmit: (String) -> Unit,
    onBiometric: () -> Unit,
    onCancel: () -> Unit,
    biometricEnabled: Boolean
) {
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x99000000)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "解锁 PassVault",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "请验证主密码以继续自动填充",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(20.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    placeholder = { Text("主密码", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    shape = RoundedCornerShape(4.dp),
                    isError = error != null,
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    )
                )

                if (error != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = { onSubmit(password) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(50),
                    enabled = password.isNotEmpty() && !loading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    Text("解锁", style = MaterialTheme.typography.labelLarge)
                }

                if (biometricEnabled) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(50))
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.surface)
                            .clickable(enabled = !loading) { onBiometric() },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fingerprint,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "使用生物识别",
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .clickable { onCancel() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "取消",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}
