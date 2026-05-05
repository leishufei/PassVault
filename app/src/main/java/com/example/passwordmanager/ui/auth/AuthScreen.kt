package com.example.passwordmanager.ui.auth

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Lock
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
import com.example.passwordmanager.ui.theme.DarkSurface
import com.example.passwordmanager.ui.theme.PrimaryPurple
import com.example.passwordmanager.ui.theme.PrimaryPurpleDark

@Composable
fun LogoHeader() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(PrimaryPurple, PrimaryPurpleDark)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier.size(50.dp),
                tint = Color.White
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "SafeVault",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "安全存储 · 随时访问",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    isPasswordVisible: Boolean,
    onVisibilityChange: () -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder) },
        modifier = Modifier.fillMaxWidth(),
        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = onVisibilityChange) {
                Icon(
                    imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = DarkSurface,
            unfocusedContainerColor = DarkSurface,
            disabledContainerColor = DarkSurface,
            focusedBorderColor = PrimaryPurple,
            unfocusedBorderColor = Color.Transparent,
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
            .padding(32.dp),
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
            onVisibilityChange = { passwordVisible = !passwordVisible }
        )

        Spacer(modifier = Modifier.height(16.dp))

        AuthTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            placeholder = "确认主密码",
            isPasswordVisible = passwordVisible,
            onVisibilityChange = { passwordVisible = !passwordVisible }
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
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
            enabled = password.isNotEmpty() && confirmPassword.isNotEmpty()
        ) {
            Text("开始使用", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun LoginScreen(
    onLogin: (String) -> Unit,
    onBiometricClick: () -> Unit,
    isBiometricAvailable: Boolean,
    isBiometricEnabled: Boolean,
    error: String?
) {
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    LaunchedEffect(isBiometricAvailable, isBiometricEnabled) {
        if (isBiometricAvailable && isBiometricEnabled) {
            onBiometricClick()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        LogoHeader()
        Spacer(modifier = Modifier.height(48.dp))

        AuthTextField(
            value = password,
            onValueChange = { password = it },
            placeholder = "输入主密码",
            isPasswordVisible = passwordVisible,
            onVisibilityChange = { passwordVisible = !passwordVisible }
        )

        error?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { onLogin(password) },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
            enabled = password.isNotEmpty()
        ) {
            Text("解锁", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        if (isBiometricAvailable && isBiometricEnabled) {
            Spacer(modifier = Modifier.height(48.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onBiometricClick() }) {
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("使用指纹解锁", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = "忘记主密码？",
            color = PrimaryPurple,
            modifier = Modifier
                .padding(bottom = 16.dp)
                .clickable { /* TODO */ },
            fontWeight = FontWeight.Medium
        )
    }
}
