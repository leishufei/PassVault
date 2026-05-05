package com.example.passwordmanager.ui.passwords

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.passwordmanager.domain.generator.PasswordConfig
import com.example.passwordmanager.ui.theme.BrandColors
import com.example.passwordmanager.ui.theme.DarkSurface
import com.example.passwordmanager.ui.theme.PrimaryPurple
import com.example.passwordmanager.ui.theme.SuccessGreen
import com.example.passwordmanager.ui.theme.WarningYellow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordDetailScreen(
    passwordId: Long?,
    viewModel: PasswordViewModel,
    onNavigateBack: () -> Unit,
    onPasswordSaved: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var website by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var isFavorite by remember { mutableStateOf(false) }
    var iconColor by remember { mutableStateOf(0) }
    var createdAt by remember { mutableStateOf(0L) }
    var updatedAt by remember { mutableStateOf(0L) }
    var passwordVisible by remember { mutableStateOf(false) }
    
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showGeneratorDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    val clipboardManager = LocalClipboardManager.current
    val isNewPassword = passwordId == null

    LaunchedEffect(passwordId) {
        passwordId?.let { id ->
            viewModel.passwords.collect { passwords ->
                passwords.find { it.id == id }?.let { entry ->
                    title = entry.title
                    username = entry.username
                    password = entry.password
                    website = entry.website
                    notes = entry.notes
                    isFavorite = entry.isFavorite
                    iconColor = entry.iconColor
                    createdAt = entry.createdAt
                    updatedAt = entry.updatedAt
                }
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
                Text(
                    text = if (isNewPassword) "添加密码" else "",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
            },
            actions = {
                if (!isNewPassword) {
                    IconButton(onClick = { /* Edit action if any */ }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.White)
                    }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More", tint = Color.White)
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier.background(DarkSurface)
                        ) {
                            DropdownMenuItem(
                                text = { Text("删除密码", color = Color.Red) },
                                onClick = { 
                                    showMenu = false
                                    showDeleteDialog = true 
                                }
                            )
                        }
                    }
                } else {
                    IconButton(
                        onClick = {
                            viewModel.savePassword(passwordId, title, username, password, website, notes, iconColor, 0)
                            onPasswordSaved()
                        },
                        enabled = title.isNotEmpty() && username.isNotEmpty() && password.isNotEmpty()
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Save", tint = PrimaryPurple)
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Icon
            val colorIndex = if (iconColor != 0) iconColor else abs(title.hashCode()) % BrandColors.size
            val bgColor = BrandColors[colorIndex]
            
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (title.isEmpty()) DarkSurface else bgColor.copy(alpha = 0.2f))
                    .clickable { 
                        if (title.isNotEmpty()) {
                            iconColor = (iconColor + 1) % BrandColors.size
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (title.isNotEmpty()) {
                    Text(
                        text = title.take(1).uppercase(),
                        color = bgColor,
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (!isNewPassword) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = {
                        isFavorite = !isFavorite
                        passwordId?.let { viewModel.toggleFavorite(it, isFavorite) }
                    }) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Favorite",
                            tint = if (isFavorite) WarningYellow else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Inputs
            if (isNewPassword) {
                DetailTextField(value = title, onValueChange = { title = it }, label = "名称", placeholder = "例如: Google")
                Spacer(modifier = Modifier.height(16.dp))
            }

            DetailTextField(
                value = username,
                onValueChange = { username = it },
                label = "用户名",
                placeholder = "例如: user@gmail.com",
                onCopy = { clipboardManager.setText(AnnotatedString(username)) }
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurface, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Text("密码", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        modifier = Modifier.weight(1f),
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent
                        )
                    )
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { clipboardManager.setText(AnnotatedString(password)) }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                
                // Strength bar
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("强度: ", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val strengthText = if (password.length > 10) "强" else if (password.length > 6) "中" else "弱"
                    val strengthColor = if (password.length > 10) SuccessGreen else if (password.length > 6) WarningYellow else Color.Red
                    Text(strengthText, fontSize = 12.sp, color = strengthColor, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .height(4.dp)
                            .weight(1f)
                            .background(DarkSurface, RoundedCornerShape(2.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(if (password.length > 10) 0.8f else if (password.length > 6) 0.5f else 0.2f)
                                .height(4.dp)
                                .background(strengthColor, RoundedCornerShape(2.dp))
                        )
                    }
                }

                if (isNewPassword) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { showGeneratorDialog = true },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                    ) {
                        Text("生成密码", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            DetailTextField(
                value = website,
                onValueChange = { website = it },
                label = "网址",
                placeholder = "https://example.com",
                onCopy = { clipboardManager.setText(AnnotatedString(website)) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            DetailTextField(
                value = notes,
                onValueChange = { notes = it },
                label = "备注",
                placeholder = "其他信息...",
                singleLine = false
            )

            if (!isNewPassword && createdAt > 0) {
                Spacer(modifier = Modifier.height(24.dp))
                val sdf = SimpleDateFormat("yyyy年MM月dd日 HH:mm", Locale.getDefault())
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("创建时间", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(sdf.format(Date(createdAt)), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("更新时间", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(sdf.format(Date(updatedAt)), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        viewModel.savePassword(passwordId, title, username, password, website, notes, iconColor, 0)
                        onPasswordSaved()
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                ) {
                    Text("保存修改", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = DarkSurface,
            title = { Text("删除密码", color = Color.White) },
            text = { Text("确定要删除此密码吗？", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            confirmButton = {
                TextButton(
                    onClick = {
                        passwordId?.let { viewModel.deletePassword(it) }
                        showDeleteDialog = false
                        onNavigateBack()
                    }
                ) {
                    Text("删除", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("取消", color = Color.White)
                }
            }
        )
    }

    if (showGeneratorDialog) {
        PasswordGeneratorDialog(
            onDismiss = { showGeneratorDialog = false },
            onGenerate = { generatedPassword ->
                password = generatedPassword
                showGeneratorDialog = false
            },
            viewModel = viewModel
        )
    }
}

@Composable
fun DetailTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    singleLine: Boolean = true,
    onCopy: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkSurface, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Text(label, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = { Text(placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                modifier = Modifier.weight(1f),
                singleLine = singleLine,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )
            if (onCopy != null) {
                IconButton(onClick = onCopy) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun PasswordGeneratorDialog(
    onDismiss: () -> Unit,
    onGenerate: (String) -> Unit,
    viewModel: PasswordViewModel
) {
    var length by remember { mutableStateOf(16) }
    var includeUppercase by remember { mutableStateOf(true) }
    var includeLowercase by remember { mutableStateOf(true) }
    var includeNumbers by remember { mutableStateOf(true) }
    var includeSymbols by remember { mutableStateOf(true) }
    var generatedPassword by remember { mutableStateOf("") }

    LaunchedEffect(length, includeUppercase, includeLowercase, includeNumbers, includeSymbols) {
        try {
            generatedPassword = viewModel.generatePassword(
                PasswordConfig(length, includeUppercase, includeLowercase, includeNumbers, includeSymbols)
            )
        } catch (e: Exception) {
            generatedPassword = ""
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = { Text("生成密码", color = Color.White) },
        text = {
            Column {
                Text(
                    text = generatedPassword,
                    style = MaterialTheme.typography.titleMedium,
                    color = PrimaryPurple,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onGenerate(generatedPassword) },
                enabled = generatedPassword.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
            ) {
                Text("使用密码")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = Color.White)
            }
        }
    )
}
