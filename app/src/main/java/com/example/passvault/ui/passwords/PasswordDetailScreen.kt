package com.example.passvault.ui.passwords

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.passvault.data.model.Category
import com.example.passvault.domain.generator.PasswordConfig
import com.example.passvault.ui.theme.BrandColors
import com.example.passvault.ui.theme.CardBackground
import com.example.passvault.ui.theme.CardBorder
import com.example.passvault.ui.theme.DarkSurface
import com.example.passvault.ui.theme.PrimaryIndigo
import com.example.passvault.ui.theme.PrimaryPurple
import com.example.passvault.ui.theme.PrimaryPurpleLight
import com.example.passvault.ui.theme.SuccessGreen
import com.example.passvault.ui.theme.TextSecondary
import com.example.passvault.ui.theme.TextTertiary
import com.example.passvault.ui.theme.WarningYellow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

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
    var categoryId by remember { mutableStateOf(0L) }
    var createdAt by remember { mutableStateOf(0L) }
    var updatedAt by remember { mutableStateOf(0L) }
    var passwordVisible by remember { mutableStateOf(false) }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showGeneratorDialog by remember { mutableStateOf(false) }

    val isNewPassword = passwordId == null
    var isEditing by remember { mutableStateOf(isNewPassword) }

    val clipboardManager = LocalClipboardManager.current
    val categories by viewModel.categories.collectAsState()

    LaunchedEffect(passwordId) {
        passwordId?.let { id ->
            viewModel.getPasswordDetail(id)?.let { entry ->
                title = entry.title
                username = entry.username
                password = entry.password
                website = entry.website
                notes = entry.notes
                isFavorite = entry.isFavorite
                iconColor = entry.iconColor
                categoryId = entry.categoryId
                createdAt = entry.createdAt
                updatedAt = entry.updatedAt
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.07f))
                    .clickable {
                        if (isEditing && !isNewPassword) isEditing = false
                        else onNavigateBack()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFFB0B0CC), modifier = Modifier.size(18.dp))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!isNewPassword && !isEditing) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.07f))
                            .clickable { isEditing = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color(0xFFB0B0CC), modifier = Modifier.size(16.dp))
                    }
                }
                if (isEditing) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (title.isNotEmpty() && password.isNotEmpty()) PrimaryPurple else Color.White.copy(alpha = 0.07f))
                            .clickable(enabled = title.isNotEmpty() && password.isNotEmpty()) {
                                if (isNewPassword) {
                                    viewModel.savePassword(null, title, username, password, website, notes, iconColor, 0, categoryId)
                                    onPasswordSaved()
                                } else {
                                    viewModel.savePassword(passwordId, title, username, password, website, notes, iconColor, 0, categoryId)
                                    isEditing = false
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Save", tint = if (title.isNotEmpty() && password.isNotEmpty()) Color.White else TextTertiary, modifier = Modifier.size(18.dp))
                    }
                }
                if (!isNewPassword) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.07f))
                            .clickable { showDeleteDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Icon & Title
            Spacer(modifier = Modifier.height(16.dp))
            val colorIndex = if (iconColor != 0) iconColor % BrandColors.size else abs(title.hashCode()) % BrandColors.size
            val bgColor = BrandColors[colorIndex]

            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(if (title.isEmpty()) DarkSurface else bgColor)
                    .clickable(enabled = isEditing) {
                        if (title.isNotEmpty()) iconColor = (iconColor + 1) % BrandColors.size
                    },
                contentAlignment = Alignment.Center
            ) {
                if (title.isNotEmpty()) {
                    Text(title.take(1).uppercase(), color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.Add, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(32.dp))
                }
            }

            if (!isEditing && title.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) WarningYellow else TextTertiary,
                        modifier = Modifier
                            .size(20.dp)
                            .clickable {
                                isFavorite = !isFavorite
                                passwordId?.let { viewModel.toggleFavorite(it, isFavorite) }
                            }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Fields
            if (isEditing) {
                EditableFields(
                    title = title, onTitleChange = { title = it },
                    username = username, onUsernameChange = { username = it },
                    password = password, onPasswordChange = { password = it },
                    website = website, onWebsiteChange = { website = it },
                    notes = notes, onNotesChange = { notes = it },
                    categoryId = categoryId, onCategoryChange = { categoryId = it },
                    categories = categories,
                    passwordVisible = passwordVisible,
                    onPasswordVisibilityChange = { passwordVisible = !passwordVisible },
                    onGenerateClick = { showGeneratorDialog = true },
                    isNewPassword = isNewPassword
                )

                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        if (isNewPassword) {
                            viewModel.savePassword(null, title, username, password, website, notes, iconColor, 0, categoryId)
                            onPasswordSaved()
                        } else {
                            viewModel.savePassword(passwordId, title, username, password, website, notes, iconColor, 0, categoryId)
                            isEditing = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    enabled = title.isNotEmpty() && password.isNotEmpty()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.linearGradient(listOf(PrimaryPurple, PrimaryIndigo)), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(if (isNewPassword) "添加密码" else "保存更改", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                }
            } else {
                // View mode - field cards
                // Category Card
                val selectedCategory = categories.find { it.id == categoryId }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CardBackground)
                        .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Text("分类", fontSize = 12.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (selectedCategory != null) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(BrandColors[selectedCategory.color % BrandColors.size])
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = selectedCategory.name,
                                fontSize = 14.sp,
                                color = Color.White
                            )
                        } else {
                            Text(
                                text = "未分类",
                                fontSize = 14.sp,
                                color = TextTertiary
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                FieldCard(label = "用户名", value = username, onCopy = { clipboardManager.setText(AnnotatedString(username)) })
                Spacer(modifier = Modifier.height(12.dp))
                PasswordFieldCard(
                    password = password,
                    visible = passwordVisible,
                    onVisibilityToggle = { passwordVisible = !passwordVisible },
                    onCopy = { clipboardManager.setText(AnnotatedString(password)) }
                )
                if (website.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    FieldCard(label = "网址", value = website, isLink = true, onCopy = { clipboardManager.setText(AnnotatedString(website)) })
                }
                if (notes.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    FieldCard(label = "备注", value = notes)
                }
                if (createdAt > 0) {
                    Spacer(modifier = Modifier.height(12.dp))
                    TimestampCard(createdAt = createdAt, updatedAt = updatedAt)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = DarkSurface,
            shape = RoundedCornerShape(24.dp),
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEF4444).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(28.dp))
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("删除 $title?", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Text("此操作不可撤销，密码将被永久删除。", color = TextSecondary, fontSize = 14.sp)
            },
            confirmButton = {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextButton(
                        onClick = { showDeleteDialog = false },
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.textButtonColors(containerColor = Color.White.copy(alpha = 0.08f))
                    ) { Text("取消", color = Color(0xFFD0D0F0), fontSize = 15.sp) }
                    TextButton(
                        onClick = {
                            passwordId?.let { viewModel.deletePassword(it) }
                            showDeleteDialog = false
                            onNavigateBack()
                        },
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.textButtonColors(containerColor = Color(0xFFEF4444))
                    ) { Text("删除", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold) }
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
fun FieldCard(
    label: String,
    value: String,
    isLink: Boolean = false,
    onCopy: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBackground)
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Text(label, fontSize = 12.sp, color = TextSecondary)
        Spacer(modifier = Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = value,
                fontSize = 14.sp,
                color = if (isLink) Color(0xFF818CF8) else Color.White,
                modifier = Modifier.weight(1f)
            )
            if (onCopy != null) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .clickable { onCopy() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = TextSecondary, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}

@Composable
fun PasswordFieldCard(
    password: String,
    visible: Boolean,
    onVisibilityToggle: () -> Unit,
    onCopy: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBackground)
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Text("密码", fontSize = 12.sp, color = TextSecondary)
        Spacer(modifier = Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (visible) password else "••••••••••••••",
                fontSize = 15.sp,
                color = Color.White,
                letterSpacing = if (visible) 0.sp else 3.sp,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onVisibilityToggle, modifier = Modifier.size(28.dp)) {
                Icon(
                    imageVector = if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .clickable { onCopy() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = TextSecondary, modifier = Modifier.size(14.dp))
            }
        }

        // Strength bar
        Spacer(modifier = Modifier.height(12.dp))
        val strengthText = when {
            password.length > 12 -> "很强"
            password.length > 8 -> "中等"
            else -> "较弱"
        }
        val strengthColor = when {
            password.length > 12 -> SuccessGreen
            password.length > 8 -> WarningYellow
            else -> Color(0xFFEF4444)
        }
        val strengthPercent = when {
            password.length > 12 -> 1.0f
            password.length > 8 -> 0.6f
            else -> 0.3f
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("强度:", fontSize = 12.sp, color = TextSecondary)
            Text(strengthText, fontSize = 12.sp, color = strengthColor, fontWeight = FontWeight.SemiBold)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color.White.copy(alpha = 0.1f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(strengthPercent)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(strengthColor)
            )
        }
    }
}

@Composable
fun TimestampCard(createdAt: Long, updatedAt: Long) {
    val sdf = SimpleDateFormat("yyyy年MM月dd日 HH:mm", Locale.getDefault())
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.03f))
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("创建时间", fontSize = 12.sp, color = TextSecondary)
            Text(sdf.format(Date(createdAt)), fontSize = 12.sp, color = TextTertiary)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("更新时间", fontSize = 12.sp, color = TextSecondary)
            Text(sdf.format(Date(updatedAt)), fontSize = 12.sp, color = TextTertiary)
        }
    }
}

@Composable
fun EditableFields(
    title: String, onTitleChange: (String) -> Unit,
    username: String, onUsernameChange: (String) -> Unit,
    password: String, onPasswordChange: (String) -> Unit,
    website: String, onWebsiteChange: (String) -> Unit,
    notes: String, onNotesChange: (String) -> Unit,
    categoryId: Long, onCategoryChange: (Long) -> Unit,
    categories: List<com.example.passvault.data.model.Category>,
    passwordVisible: Boolean,
    onPasswordVisibilityChange: () -> Unit,
    onGenerateClick: () -> Unit,
    isNewPassword: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (isNewPassword) {
            DetailTextField(value = title, onValueChange = onTitleChange, label = "名称", placeholder = "例如: Google")
        }
        DetailTextField(value = username, onValueChange = onUsernameChange, label = "用户名", placeholder = "例如: user@gmail.com")

        CategorySelectionField(
            selectedCategoryId = categoryId,
            categories = categories,
            onCategorySelected = onCategoryChange
        )

        // Password field with generate
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CardBackground)
                .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Text("密码", fontSize = 12.sp, color = TextSecondary)
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.weight(1f)) {
                    BasicTextField(
                        value = password,
                        onValueChange = onPasswordChange,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(
                            color = Color.White,
                            fontSize = 14.sp
                        ),
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        decorationBox = { innerTextField ->
                            if (password.isEmpty()) {
                                Text("输入或生成密码", color = TextTertiary, fontSize = 14.sp)
                            }
                            innerTextField()
                        }
                    )
                }
                IconButton(onClick = onPasswordVisibilityChange, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(PrimaryPurple.copy(alpha = 0.2f))
                    .clickable { onGenerateClick() }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("生成密码", color = PrimaryPurpleLight, fontSize = 12.sp)
            }

            if (password.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                val strengthText = when {
                    password.length > 12 -> "很强"
                    password.length > 8 -> "中等"
                    else -> "较弱"
                }
                val strengthColor = when {
                    password.length > 12 -> SuccessGreen
                    password.length > 8 -> WarningYellow
                    else -> Color(0xFFEF4444)
                }
                val strengthPercent = when {
                    password.length > 12 -> 1.0f
                    password.length > 8 -> 0.6f
                    else -> 0.3f
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("强度:", fontSize = 12.sp, color = TextSecondary)
                    Text(strengthText, fontSize = 12.sp, color = strengthColor, fontWeight = FontWeight.SemiBold)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(Color.White.copy(alpha = 0.1f))
                ) {
                    Box(modifier = Modifier.fillMaxWidth(strengthPercent).height(6.dp).clip(RoundedCornerShape(3.dp)).background(strengthColor))
                }
            }
        }

        DetailTextField(value = website, onValueChange = onWebsiteChange, label = "网址", placeholder = "https://example.com")
        DetailTextField(value = notes, onValueChange = onNotesChange, label = "备注", placeholder = "备注信息...", singleLine = false)
    }
}

@Composable
fun DetailTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    singleLine: Boolean = true,
    readOnly: Boolean = false,
    onCopy: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBackground)
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Text(label, fontSize = 12.sp, color = TextSecondary)
        Spacer(modifier = Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.weight(1f)) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(
                        color = Color.White,
                        fontSize = 14.sp
                    ),
                    singleLine = singleLine,
                    readOnly = readOnly,
                    decorationBox = { innerTextField ->
                        if (value.isEmpty()) {
                            Text(placeholder, color = TextTertiary, fontSize = 14.sp)
                        }
                        innerTextField()
                    }
                )
            }
            if (onCopy != null && value.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .clickable { onCopy() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = TextSecondary, modifier = Modifier.size(14.dp))
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
        shape = RoundedCornerShape(24.dp),
        title = { Text("生成密码", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    text = generatedPassword,
                    style = MaterialTheme.typography.titleMedium,
                    color = PrimaryPurpleLight,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onGenerate(generatedPassword) },
                enabled = generatedPassword.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                shape = RoundedCornerShape(12.dp)
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

@Composable
fun CategorySelectionField(
    selectedCategoryId: Long,
    categories: List<com.example.passvault.data.model.Category>,
    onCategorySelected: (Long) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedCategory = categories.find { it.id == selectedCategoryId }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBackground)
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
            .clickable { expanded = true }
            .padding(16.dp)
    ) {
        Text("分类", fontSize = 12.sp, color = TextSecondary)
        Spacer(modifier = Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (selectedCategory != null) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(BrandColors[selectedCategory.color % BrandColors.size])
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = selectedCategory.name,
                    fontSize = 14.sp,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
            } else {
                Text(
                    text = "选择分类",
                    fontSize = 14.sp,
                    color = TextTertiary,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(DarkSurface, RoundedCornerShape(16.dp))
        ) {
            categories.forEach { category ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(BrandColors[category.color % BrandColors.size])
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(category.name, color = Color.White, fontSize = 14.sp)
                        }
                    },
                    onClick = {
                        onCategorySelected(category.id)
                        expanded = false
                    }
                )
            }
        }
    }
}
