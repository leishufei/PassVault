package com.example.passvault.ui.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.passvault.ui.theme.DarkSurface
import com.example.passvault.ui.theme.PrimaryPurple
import com.example.passvault.ui.theme.SuccessGreen
import com.example.passvault.ui.theme.WarningYellow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    var showSafeDialog by remember { mutableStateOf(false) }
    var showAutoLockDialog by remember { mutableStateOf(false) }
    var showMasterPasswordDialog by remember { mutableStateOf(false) }

    val autoLockOptions = listOf("立即", "1分钟", "5分钟", "10分钟", "永不")

    val backupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            viewModel.exportBackup(uri)
                .onSuccess {
                    Toast.makeText(context, "备份已导出", Toast.LENGTH_SHORT).show()
                }
                .onFailure {
                    Toast.makeText(context, "备份失败: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            viewModel.importBackup(uri)
                .onSuccess {
                    Toast.makeText(context, "数据恢复成功", Toast.LENGTH_SHORT).show()
                }
                .onFailure {
                    Toast.makeText(context, "恢复失败: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = {
                Text("设置", color = Color.White, fontWeight = FontWeight.Bold)
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text("安全", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurface, RoundedCornerShape(16.dp))
            ) {
                SettingItem(icon = Icons.Default.Security, iconTint = PrimaryPurple, title = "保险箱", subtitle = "安全评分 90/100") {
                    showSafeDialog = true
                }
                SettingItem(icon = Icons.Default.Timer, title = "自动锁定", value = uiState.autoLockTime) {
                    showAutoLockDialog = true
                }
                SettingItem(icon = Icons.Default.Lock, title = "主密码", subtitle = "更改主密码") {
                    showMasterPasswordDialog = true
                }
                SettingSwitchItem(
                    icon = Icons.Default.Fingerprint,
                    title = "生物识别解锁",
                    subtitle = "使用指纹或面部解锁",
                    checked = uiState.biometricEnabled,
                    onCheckedChange = { viewModel.setBiometricEnabled(it) }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text("数据", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurface, RoundedCornerShape(16.dp))
            ) {
                SettingItem(icon = Icons.Default.Backup, title = "备份", subtitle = "导出备份数据") {
                    backupLauncher.launch("SafeVault_Backup.json")
                }
                SettingItem(icon = Icons.Default.Restore, title = "备份恢复", subtitle = "从备份文件恢复数据") {
                    restoreLauncher.launch(arrayOf("application/json"))
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showSafeDialog) {
        val stats = uiState.passwordStats
        AlertDialog(
            onDismissRequest = { showSafeDialog = false },
            title = { Text("安全评分分析", color = Color.White) },
            text = {
                if (stats == null) {
                    Text("加载中...", color = Color.White)
                } else if (stats.totalCount == 0) {
                    Text("暂无密码数据", color = Color.White)
                } else {
                    Column {
                        Text("综合评分: ${stats.averageStrength}/100", color = SuccessGreen, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("• 强密码: ${stats.strongCount}", color = Color.White)
                        Text("• 中等密码: ${stats.totalCount - stats.strongCount - stats.weakCount}", color = Color.White)
                        Text("• 弱密码: ${stats.weakCount}", color = Color.White)
                        Text("• 重复使用: ${stats.duplicateCount}", color = if (stats.duplicateCount > 0) WarningYellow else Color.White)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSafeDialog = false }) { Text("确定", color = PrimaryPurple) }
            },
            containerColor = DarkSurface
        )
    }

    if (showAutoLockDialog) {
        AlertDialog(
            onDismissRequest = { showAutoLockDialog = false },
            title = { Text("自动锁定时间", color = Color.White) },
            text = {
                Column {
                    autoLockOptions.forEach { option ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setAutoLockTime(option)
                                    showAutoLockDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = uiState.autoLockTime == option,
                                onClick = {
                                    viewModel.setAutoLockTime(option)
                                    showAutoLockDialog = false
                                }
                            )
                            Text(text = option, color = Color.White, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            },
            confirmButton = {},
            containerColor = DarkSurface
        )
    }

    if (showMasterPasswordDialog) {
        var oldPassword by remember { mutableStateOf("") }
        var newPassword by remember { mutableStateOf("") }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showMasterPasswordDialog = false },
            title = { Text("修改主密码", color = Color.White) },
            text = {
                Column {
                    OutlinedTextField(
                        value = oldPassword,
                        onValueChange = { oldPassword = it },
                        label = { Text("旧密码", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        visualTransformation = PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("新密码", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        visualTransformation = PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    errorMessage?.let {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(it, color = Color.Red, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (oldPassword.isEmpty() || newPassword.isEmpty()) {
                            errorMessage = "密码不能为空"
                        } else {
                            viewModel.changeMasterPassword(oldPassword, newPassword)
                                .onSuccess {
                                    Toast.makeText(context, "主密码修改成功", Toast.LENGTH_SHORT).show()
                                    showMasterPasswordDialog = false
                                }
                                .onFailure {
                                    errorMessage = it.message
                                }
                        }
                    }
                ) { Text("保存", color = PrimaryPurple) }
            },
            dismissButton = {
                TextButton(onClick = { showMasterPasswordDialog = false }) { Text("取消", color = Color.White) }
            },
            containerColor = DarkSurface
        )
    }
}

@Composable
fun SettingItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    title: String,
    subtitle: String? = null,
    value: String? = null,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(24.dp))
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 16.sp)
            if (subtitle != null) {
                Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
        }
        
        if (value != null) {
            Text(value, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
            Spacer(modifier = Modifier.width(8.dp))
        }
        
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun SettingSwitchItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 16.sp)
            if (subtitle != null) {
                Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
        }
        
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = PrimaryPurple,
                uncheckedThumbColor = Color.LightGray,
                uncheckedTrackColor = MaterialTheme.colorScheme.background
            )
        )
    }
}
