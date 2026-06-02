package com.example.passvault.ui.settings

import androidx.activity.result.ActivityResult
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.passvault.ui.theme.CardBackground
import com.example.passvault.ui.theme.CardBorder
import com.example.passvault.ui.theme.DialogBackground
import com.example.passvault.ui.theme.DividerColor
import com.example.passvault.ui.theme.LightSurfaceVariant
import com.example.passvault.ui.theme.PrimaryBlue
import com.example.passvault.ui.theme.SuccessGreen
import com.example.passvault.ui.theme.TextPrimary
import com.example.passvault.ui.theme.TextSecondary
import com.example.passvault.ui.theme.TextTertiary
import com.example.passvault.ui.theme.WarningYellow

@Composable
fun SettingsScreen(
    onLock: () -> Unit,
    onNavigateToCategoryManagement: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val shouldLockNow by viewModel.shouldLockNow.collectAsState()

    LaunchedEffect(shouldLockNow) {
        if (shouldLockNow) {
            viewModel.resetLockNowFlag()
            onLock()
        }
    }

    var showSafeDialog by remember { mutableStateOf(false) }
    var showAutoLockDialog by remember { mutableStateOf(false) }
    var showMasterPasswordDialog by remember { mutableStateOf(false) }

    val autoLockOptions = listOf("立即", "1分钟", "5分钟", "10分钟", "永不")

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        viewModel.handleGoogleSignInResult(result.data)
    }

    val backupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) {
            viewModel.exportCsv(uri)
                .onSuccess { Toast.makeText(context, "密码已导出", Toast.LENGTH_SHORT).show() }
                .onFailure { Toast.makeText(context, "导出失败: ${it.message}", Toast.LENGTH_SHORT).show() }
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            viewModel.importCsv(uri)
                .onSuccess { Toast.makeText(context, "数据导入成功", Toast.LENGTH_SHORT).show() }
                .onFailure { Toast.makeText(context, "导入失败: ${it.message}", Toast.LENGTH_SHORT).show() }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Text(
            text = "设置",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            // Security section
            SectionTitle("安全")
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardBackground)
                    .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
            ) {
                SettingItem(
                    icon = Icons.Default.Security,
                    iconBg = SuccessGreen.copy(alpha = 0.15f),
                    iconTint = SuccessGreen,
                    title = "保险库",
                    subtitle = "查看安全评分",
                    onClick = {
                        viewModel.loadPasswordStatisticsIfNeeded()
                        showSafeDialog = true
                    }
                )
                SettingDivider()
                SettingItem(
                    icon = Icons.Default.Timer,
                    iconBg = WarningYellow.copy(alpha = 0.15f),
                    iconTint = WarningYellow,
                    title = "自动锁定",
                    subtitle = uiState.autoLockTime,
                    onClick = { showAutoLockDialog = true }
                )
                SettingDivider()
                SettingItem(
                    icon = Icons.Default.Lock,
                    iconBg = Color(0xFF0284C7).copy(alpha = 0.15f),
                    iconTint = Color(0xFF0284C7),
                    title = "主密码",
                    subtitle = "修改主密码",
                    onClick = { showMasterPasswordDialog = true }
                )
                SettingDivider()
                SettingSwitchItem(
                    icon = Icons.Default.Fingerprint,
                    iconBg = PrimaryBlue.copy(alpha = 0.15f),
                    iconTint = PrimaryBlue,
                    title = "生物识别解锁",
                    subtitle = "使用指纹或面部解锁",
                    checked = uiState.biometricEnabled,
                    onCheckedChange = { viewModel.setBiometricEnabled(it) }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Data section
            SectionTitle("数据")
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardBackground)
                    .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
            ) {
                SettingItem(
                    icon = Icons.Default.Backup,
                    iconBg = SuccessGreen.copy(alpha = 0.15f),
                    iconTint = SuccessGreen,
                    title = "导出密码",
                    subtitle = "导出为 CSV 格式",
                    onClick = { backupLauncher.launch("PassVault_Backup.csv") }
                )
                SettingDivider()
                SettingItem(
                    icon = Icons.Default.Restore,
                    iconBg = Color(0xFF3B82F6).copy(alpha = 0.15f),
                    iconTint = Color(0xFF3B82F6),
                    title = "导入密码",
                    subtitle = "从 CSV 导入密码",
                    onClick = { restoreLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "text/plain")) }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            SectionTitle("管理")
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardBackground)
                    .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
            ) {
                SettingItem(
                    icon = Icons.Default.Security,
                    iconBg = Color(0xFF8B5CF6).copy(alpha = 0.15f),
                    iconTint = Color(0xFF8B5CF6),
                    title = "分类管理",
                    subtitle = "管理密码分类",
                    onClick = onNavigateToCategoryManagement
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            SectionTitle("云同步")
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardBackground)
                    .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
            ) {
                SettingItem(
                    icon = if (uiState.currentProvider != null) Icons.Default.CloudDone else Icons.Default.CloudOff,
                    iconBg = if (uiState.currentProvider != null) SuccessGreen.copy(alpha = 0.15f) else Color(0xFF3B82F6).copy(alpha = 0.15f),
                    iconTint = if (uiState.currentProvider != null) SuccessGreen else Color(0xFF3B82F6),
                    title = if (uiState.currentProvider != null) "已绑定 ${uiState.currentProvider}" else "绑定 Google Drive",
                    subtitle = "实现跨设备密码无缝同步",
                    onClick = {
                        if (uiState.currentProvider == null) {
                            googleSignInLauncher.launch(viewModel.getGoogleSignInIntent())
                        }
                    }
                )
                SettingDivider()
                SettingItem(
                    icon = Icons.Default.CloudSync,
                    iconBg = PrimaryBlue.copy(alpha = 0.15f),
                    iconTint = PrimaryBlue,
                    title = "立即同步",
                    subtitle = uiState.lastSyncMessage ?: "与云端进行双向合并同步",
                    onClick = { viewModel.syncNow() },
                    trailing = {
                        if (uiState.isSyncing) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = PrimaryBlue, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(16.dp))
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // App section
            SectionTitle("应用")
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardBackground)
                    .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
            ) {
                SettingItem(
                    icon = Icons.Default.Shield,
                    iconBg = PrimaryBlue.copy(alpha = 0.15f),
                    iconTint = PrimaryBlue,
                    title = "关于 PassVault",
                    subtitle = "版本 1.0.0",
                    trailing = { Text("1.0.0", color = TextTertiary, fontSize = 13.sp) }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Lock button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFFEF2F2))
                    .border(1.dp, Color(0xFFFECACA), RoundedCornerShape(16.dp))
                    .clickable { onLock() },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                    Text("立即锁定", color = Color(0xFFEF4444), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Security dialog
    if (showSafeDialog) {
        val stats = uiState.passwordStats
        AlertDialog(
            onDismissRequest = { showSafeDialog = false },
            title = { Text("安全评分分析", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                if (stats == null) {
                    Text("加载中...", color = TextSecondary)
                } else if (stats.totalCount == 0) {
                    Text("暂无密码数据", color = TextSecondary)
                } else {
                    Column {
                        Text("综合评分: ${stats.averageStrength}/100", color = SuccessGreen, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("• 强密码: ${stats.strongCount}", color = TextPrimary)
                        Text("• 中等密码: ${stats.totalCount - stats.strongCount - stats.weakCount}", color = TextPrimary)
                        Text("• 弱密码: ${stats.weakCount}", color = TextPrimary)
                        Text("• 重复使用: ${stats.duplicateCount}", color = if (stats.duplicateCount > 0) WarningYellow else TextPrimary)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSafeDialog = false }) { Text("确定", color = PrimaryBlue) }
            },
            containerColor = DialogBackground,
            shape = RoundedCornerShape(24.dp)
        )
    }

    // Auto lock dialog
    if (showAutoLockDialog) {
        AlertDialog(
            onDismissRequest = { showAutoLockDialog = false },
            title = { Text("自动锁定时间", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    autoLockOptions.forEach { option ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (uiState.autoLockTime == option) LightSurfaceVariant
                                    else Color.Transparent
                                )
                                .clickable {
                                    viewModel.setAutoLockTime(option)
                                    showAutoLockDialog = false
                                }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = option,
                                color = if (uiState.autoLockTime == option) PrimaryBlue else TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = if (uiState.autoLockTime == option) FontWeight.SemiBold else FontWeight.Normal
                            )
                            if (uiState.autoLockTime == option) {
                                Icon(Icons.Default.Security, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            containerColor = DialogBackground,
            shape = RoundedCornerShape(24.dp)
        )
    }

    // Master password dialog
    if (showMasterPasswordDialog) {
        var oldPassword by remember { mutableStateOf("") }
        var newPassword by remember { mutableStateOf("") }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showMasterPasswordDialog = false },
            title = { Text("修改主密码", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = oldPassword,
                        onValueChange = { oldPassword = it },
                        label = { Text("当前密码", color = TextSecondary) },
                        visualTransformation = PasswordVisualTransformation(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = LightSurfaceVariant,
                            unfocusedContainerColor = LightSurfaceVariant,
                            focusedBorderColor = PrimaryBlue,
                            unfocusedBorderColor = Color.Transparent
                        )
                    )
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("新密码", color = TextSecondary) },
                        visualTransformation = PasswordVisualTransformation(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = LightSurfaceVariant,
                            unfocusedContainerColor = LightSurfaceVariant,
                            focusedBorderColor = PrimaryBlue,
                            unfocusedBorderColor = Color.Transparent
                        )
                    )
                    errorMessage?.let {
                        Text(it, color = Color.Red, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextButton(onClick = { showMasterPasswordDialog = false }) {
                        Text("取消", color = TextSecondary)
                    }
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
                                    .onFailure { errorMessage = it.message }
                            }
                        }
                    ) { Text("确认", color = PrimaryBlue, fontWeight = FontWeight.SemiBold) }
                }
            },
            containerColor = DialogBackground,
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        color = TextSecondary,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(1.dp)
            .background(DividerColor)
    )
}

@Composable
fun SettingItem(
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit = {},
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            if (subtitle != null) {
                Text(subtitle, color = TextSecondary, fontSize = 13.sp)
            }
        }

        if (trailing != null) {
            trailing()
        } else {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
fun SettingSwitchItem(
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            if (subtitle != null) {
                Text(subtitle, color = TextSecondary, fontSize = 13.sp)
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = PrimaryBlue,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFFE2E8F0)
            )
        )
    }
}
