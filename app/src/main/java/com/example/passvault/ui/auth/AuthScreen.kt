package com.example.passvault.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.passvault.ui.theme.PrimaryBlue
import com.example.passvault.ui.theme.PrimaryBlueLight
import com.example.passvault.ui.theme.TextPrimary
import com.example.passvault.ui.theme.TextSecondary
import com.example.passvault.ui.theme.LightSurfaceVariant
import com.example.passvault.ui.theme.CardBorder

@Composable
fun LogoHeader() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(PrimaryBlue, PrimaryBlueLight)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = null,
                modifier = Modifier.size(44.dp),
                tint = Color.White
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "PassVault",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            letterSpacing = 0.5.sp
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "安全存储 · 随时访问",
            fontSize = 14.sp,
            color = TextSecondary
        )
    }
}

@Composable
fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    isPasswordVisible: Boolean,
    onVisibilityChange: () -> Unit,
    isError: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = TextSecondary) },
        modifier = Modifier.fillMaxWidth(),
        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = onVisibilityChange) {
                Icon(
                    imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = null,
                    tint = TextSecondary
                )
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = LightSurfaceVariant,
            unfocusedContainerColor = LightSurfaceVariant,
            disabledContainerColor = LightSurfaceVariant,
            focusedBorderColor = if (isError) MaterialTheme.colorScheme.error else PrimaryBlue,
            unfocusedBorderColor = if (isError) MaterialTheme.colorScheme.error else CardBorder,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary
        )
    )
}

@Composable
fun SetupScreen(
    onSetPassword: (String, String) -> Unit,
    error: String?
) {
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        LogoHeader()
        Spacer(modifier = Modifier.height(48.dp))

        AuthTextField(
            value = password,
            onValueChange = { password = it },
            placeholder = "设置主密码",
            isPasswordVisible = passwordVisible,
            onVisibilityChange = { passwordVisible = !passwordVisible },
            isError = error != null
        )

        Spacer(modifier = Modifier.height(16.dp))

        AuthTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            placeholder = "确认主密码",
            isPasswordVisible = passwordVisible,
            onVisibilityChange = { passwordVisible = !passwordVisible },
            isError = error != null
        )

        error?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { onSetPassword(password, confirmPassword) },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            enabled = password.isNotEmpty() && confirmPassword.isNotEmpty()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(listOf(PrimaryBlue, PrimaryBlueLight)),
                        RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("开始使用", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            }
        }
    }
}

@Composable
fun LoginScreen(
    onLogin: (String) -> Unit,
    onBiometricClick: () -> Unit,
    isBiometricAvailable: Boolean,
    isBiometricEnabled: Boolean,
    error: String?,
    syncStatus: SyncStatus = SyncStatus.Idle
) {
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var hasAutoTriggeredBiometric by remember { mutableStateOf(false) }

    LaunchedEffect(isBiometricAvailable, isBiometricEnabled) {
        if (isBiometricAvailable && isBiometricEnabled && !hasAutoTriggeredBiometric) {
            hasAutoTriggeredBiometric = true
            onBiometricClick()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(0.15f))

        LogoHeader()

        Spacer(modifier = Modifier.weight(0.1f))

        AuthTextField(
            value = password,
            onValueChange = { password = it },
            placeholder = "输入主密码",
            isPasswordVisible = passwordVisible,
            onVisibilityChange = { passwordVisible = !passwordVisible },
            isError = error != null
        )

        error?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        // Sync status indicator
        when (syncStatus) {
            SyncStatus.Syncing -> {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "正在同步...",
                    color = PrimaryBlue,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
            }
            SyncStatus.Success -> {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "同步成功",
                    color = Color(0xFF10B981),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
            }
            SyncStatus.Error -> {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "同步失败",
                    color = Color(0xFFEF4444),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
            }
            SyncStatus.Idle -> { /* No sync status to show */ }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { onLogin(password) },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent
            ),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 0.dp,
                pressedElevation = 0.dp,
                disabledElevation = 0.dp,
                hoveredElevation = 0.dp,
                focusedElevation = 0.dp
            ),
            enabled = password.isNotEmpty()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(listOf(PrimaryBlue, PrimaryBlueLight)),
                        RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("解锁", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            }
        }

        Spacer(modifier = Modifier.weight(0.1f))

        if (isBiometricAvailable && isBiometricEnabled) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onBiometricClick() }
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(PrimaryBlue.copy(alpha = 0.1f))
                        .border(1.5.dp, PrimaryBlue.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = PrimaryBlue
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text("使用指纹解锁", color = TextSecondary, fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "忘记主密码?",
            color = PrimaryBlue,
            fontSize = 14.sp,
            modifier = Modifier
                .padding(bottom = 32.dp)
                .clickable { }
        )
    }
}
