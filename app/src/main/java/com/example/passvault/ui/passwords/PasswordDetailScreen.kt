package com.example.passvault.ui.passwords

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.passvault.domain.generator.PasswordConfig
import com.example.passvault.ui.theme.CategoryColors
import com.example.passvault.ui.theme.SemanticError
import com.example.passvault.ui.theme.SemanticSuccess
import com.example.passvault.ui.theme.SemanticWarning
import com.example.passvault.ui.theme.safeCategoryColor
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
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
    var categoryId by remember { mutableStateOf(0L) }
    var createdAt by remember { mutableStateOf(0L) }
    var updatedAt by remember { mutableStateOf(0L) }
    var passwordVisible by remember(passwordId) { mutableStateOf(viewModel.getPasswordDefaultVisible()) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showGeneratorDialog by remember { mutableStateOf(false) }

    val isNewPassword = passwordId == null
    var isEditing by remember { mutableStateOf(isNewPassword) }
    var reloadTrigger by remember { mutableStateOf(0) }
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val categories by viewModel.categories.collectAsState()

    LaunchedEffect(passwordId, reloadTrigger) {
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

    BackHandler(enabled = isEditing && !isNewPassword) {
        isEditing = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
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
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable {
                        if (isEditing && !isNewPassword) isEditing = false
                        else onNavigateBack()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!isNewPassword && !isEditing) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                if (isFavorite) SemanticWarning.copy(alpha = 0.12f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .clickable {
                                isFavorite = !isFavorite
                                passwordId?.let { viewModel.toggleFavorite(it, isFavorite) }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Favorite",
                            tint = if (isFavorite) SemanticWarning
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { isEditing = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                if (isEditing) {
                    val canSave = title.isNotEmpty() && (username.isNotEmpty() || password.isNotEmpty())
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                if (canSave) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .clickable(enabled = canSave) {
                                viewModel.savePassword(
                                    passwordId, title, username, password, website, notes, iconColor, 0, categoryId,
                                    onDuplicate = {
                                        Toast.makeText(context, "已存在相同记录", Toast.LENGTH_SHORT).show()
                                    },
                                    onSaved = {
                                        if (isNewPassword) {
                                            onPasswordSaved()
                                        } else {
                                            reloadTrigger++
                                            isEditing = false
                                        }
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Save",
                            tint = if (canSave) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                if (!isNewPassword) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(SemanticError.copy(alpha = 0.08f))
                            .clickable { showDeleteDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = SemanticError,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            val bgColor = safeCategoryColor(
                if (iconColor != 0) iconColor else abs(title.hashCode())
            )

            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        if (title.isEmpty()) MaterialTheme.colorScheme.surfaceVariant
                        else bgColor
                    )
                    .clickable(enabled = isEditing) {
                        if (title.isNotEmpty()) iconColor = (iconColor + 1) % CategoryColors.size
                    },
                contentAlignment = Alignment.Center
            ) {
                if (title.isNotEmpty()) {
                    Text(
                        title.take(1).uppercase(),
                        style = MaterialTheme.typography.displayLarge,
                        color = Color.White
                    )
                } else {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            if (!isEditing && title.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(title, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
            }

            Spacer(modifier = Modifier.height(24.dp))

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
                    isNewPassword = isNewPassword,
                    onAddCategory = { name, color -> viewModel.addCategory(name, color) }
                )

                Spacer(modifier = Modifier.height(24.dp))
                val canSave = title.isNotEmpty() && (username.isNotEmpty() || password.isNotEmpty())
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(50))
                        .background(
                            if (canSave) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .clickable(enabled = canSave) {
                            viewModel.savePassword(
                                passwordId, title, username, password, website, notes, iconColor, 0, categoryId,
                                onDuplicate = {
                                    Toast.makeText(context, "已存在相同记录", Toast.LENGTH_SHORT).show()
                                },
                                onSaved = {
                                    if (isNewPassword) {
                                        onPasswordSaved()
                                    } else {
                                        reloadTrigger++
                                        isEditing = false
                                    }
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (isNewPassword) "添加密码" else "保存更改",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (canSave) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                val selectedCategory = categories.find { it.id == categoryId }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.medium)
                        .padding(16.dp)
                ) {
                    Text("分类", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (selectedCategory != null) {
                            Box(
                                modifier = Modifier.size(8.dp).clip(CircleShape)
                                    .background(safeCategoryColor(selectedCategory.color))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(selectedCategory.name, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                        } else {
                            Text("未分类", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    FieldRow(label = "用户名", value = username, onCopy = { clipboardManager.setText(AnnotatedString(username)) })
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    PasswordFieldRow(
                        password = password,
                        visible = passwordVisible,
                        onVisibilityToggle = { passwordVisible = !passwordVisible },
                        onCopy = { clipboardManager.setText(AnnotatedString(password)) }
                    )
                    if (website.isNotEmpty()) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                        FieldRow(label = "网址", value = website, isLink = true, onCopy = { clipboardManager.setText(AnnotatedString(website)) })
                    }
                    if (notes.isNotEmpty()) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                        FieldRow(label = "备注", value = notes)
                    }
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
            containerColor = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.large,
            title = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(SemanticError.copy(alpha = 0.08f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            tint = SemanticError,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("删除 $title?", style = MaterialTheme.typography.titleLarge)
                }
            },
            text = {
                Text(
                    "此操作不可撤销，密码将被永久删除。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        onClick = { showDeleteDialog = false },
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = MaterialTheme.shapes.small,
                        colors = ButtonDefaults.textButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text(
                            "取消",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(
                        onClick = {
                            passwordId?.let { viewModel.deletePassword(it) }
                            showDeleteDialog = false
                            onNavigateBack()
                        },
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = MaterialTheme.shapes.small,
                        colors = ButtonDefaults.textButtonColors(containerColor = SemanticError)
                    ) {
                        Text(
                            "删除",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White
                        )
                    }
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
fun FieldRow(
    label: String,
    value: String,
    isLink: Boolean = false,
    onCopy: (() -> Unit)? = null
) {
    var copied by remember { mutableStateOf(false) }

    LaunchedEffect(copied) {
        if (copied) { delay(1500); copied = false }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                color = if (isLink) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            if (onCopy != null) {
                IconButton(
                    onClick = { onCopy(); copied = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                        contentDescription = "Copy",
                        tint = if (copied) SemanticSuccess else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun PasswordFieldRow(
    password: String,
    visible: Boolean,
    onVisibilityToggle: () -> Unit,
    onCopy: () -> Unit
) {
    var copied by remember { mutableStateOf(false) }

    LaunchedEffect(copied) {
        if (copied) { delay(1500); copied = false }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("密码", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (password.isEmpty()) "" else if (visible) password else "••••••••",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                letterSpacing = if (!visible) 2.sp else 0.sp,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onVisibilityToggle, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            IconButton(
                onClick = { onCopy(); copied = true },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                    contentDescription = "Copy",
                    tint = if (copied) SemanticSuccess else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        if (password.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            val strengthText = when {
                password.length > 12 -> "很强"
                password.length > 8 -> "中等"
                else -> "较弱"
            }
            val strengthColor = when {
                password.length > 12 -> SemanticSuccess
                password.length > 8 -> SemanticWarning
                else -> SemanticError
            }
            val strengthPercent = when {
                password.length > 12 -> 1.0f
                password.length > 8 -> 0.6f
                else -> 0.3f
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("强度:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(strengthText, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = strengthColor)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier.fillMaxWidth().height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(strengthPercent).height(4.dp)
                        .clip(RoundedCornerShape(2.dp)).background(strengthColor)
                )
            }
        }
    }
}

@Composable
fun TimestampCard(createdAt: Long, updatedAt: Long) {
    val sdf = SimpleDateFormat("yyyy年MM月dd日 HH:mm", Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("Asia/Shanghai")
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(110.dp)
        ) {
            Text(
                "创建时间",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(72.dp)
            )
            Text(
                sdf.format(Date(createdAt)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(110.dp)
        ) {
            Text(
                "更新时间",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(72.dp)
            )
            Text(
                sdf.format(Date(updatedAt)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
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
    isNewPassword: Boolean,
    onAddCategory: (String, Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        DetailTextField(value = title, onValueChange = onTitleChange, label = "名称", placeholder = "例如: Google", readOnly = !isNewPassword)

        CategorySelectionField(
            selectedCategoryId = categoryId, categories = categories,
            onCategorySelected = onCategoryChange, onAddCategory = onAddCategory
        )

        DetailTextField(value = username, onValueChange = onUsernameChange, label = "用户名", placeholder = "例如: user@gmail.com")

        Column(
            modifier = Modifier.fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.medium)
                .padding(16.dp)
        ) {
            Text("密码", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.weight(1f)) {
                    BasicTextField(
                        value = password, onValueChange = onPasswordChange,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.titleSmall.copy(color = MaterialTheme.colorScheme.onSurface),
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
                        visualTransformation = if (passwordVisible) VisualTransformation.None else LimitedPasswordVisualTransformation(),
                        singleLine = true,
                        decorationBox = { innerTextField ->
                            if (password.isEmpty()) Text("输入或生成密码", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            innerTextField()
                        }
                    )
                }
                IconButton(onClick = onPasswordVisibilityChange, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier.clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { onGenerateClick() }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text("生成强密码", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }

            if (password.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                val strengthColor = when { password.length > 12 -> SemanticSuccess; password.length > 8 -> SemanticWarning; else -> SemanticError }
                val strengthPercent = when { password.length > 12 -> 1.0f; password.length > 8 -> 0.6f; else -> 0.3f }
                val strengthText = when { password.length > 12 -> "很强"; password.length > 8 -> "中等"; else -> "较弱" }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("强度:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(strengthText, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = strengthColor)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Box(modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
                    Box(modifier = Modifier.fillMaxWidth(strengthPercent).height(4.dp).clip(RoundedCornerShape(2.dp)).background(strengthColor))
                }
            }
        }

        DetailTextField(value = website, onValueChange = onWebsiteChange, label = "网址", placeholder = "https://example.com")
        DetailTextField(value = notes, onValueChange = onNotesChange, label = "备注", placeholder = "备注信息...", singleLine = false)
    }
}

@Composable
fun DetailTextField(
    value: String, onValueChange: (String) -> Unit,
    label: String, placeholder: String,
    singleLine: Boolean = true, readOnly: Boolean = false
) {
    Column(
        modifier = Modifier.fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.medium)
            .padding(16.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(6.dp))
        BasicTextField(
            value = value, onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            textStyle = MaterialTheme.typography.titleSmall.copy(color = MaterialTheme.colorScheme.onSurface),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
            singleLine = singleLine, readOnly = readOnly,
            decorationBox = { innerTextField ->
                if (value.isEmpty()) Text(placeholder, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                innerTextField()
            }
        )
    }
}

@Composable
fun PasswordGeneratorDialog(onDismiss: () -> Unit, onGenerate: (String) -> Unit, viewModel: PasswordViewModel) {
    var length by remember { mutableStateOf(16f) }
    var includeUppercase by remember { mutableStateOf(true) }
    var includeLowercase by remember { mutableStateOf(true) }
    var includeNumbers by remember { mutableStateOf(true) }
    var includeSymbols by remember { mutableStateOf(true) }
    var generatedPassword by remember { mutableStateOf("") }

    LaunchedEffect(length, includeUppercase, includeLowercase, includeNumbers, includeSymbols) {
        try { generatedPassword = viewModel.generatePassword(PasswordConfig(length.toInt(), includeUppercase, includeLowercase, includeNumbers, includeSymbols)) }
        catch (_: Exception) { generatedPassword = "" }
    }

    val strengthColor = when { length > 12 -> SemanticSuccess; length > 8 -> SemanticWarning; else -> SemanticError }
    val strengthPercent = when { length > 12 -> 1.0f; length > 8 -> 0.6f; else -> 0.3f }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.large,
        title = { Text("生成强密码", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column {
                Text(generatedPassword, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(vertical = 8.dp))
                Box(modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
                    Box(modifier = Modifier.fillMaxWidth(strengthPercent).height(4.dp).clip(RoundedCornerShape(2.dp)).background(strengthColor))
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("长度", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                    Box(modifier = Modifier.clip(MaterialTheme.shapes.small).background(MaterialTheme.colorScheme.surfaceVariant).padding(horizontal = 12.dp, vertical = 4.dp)) {
                        Text(length.toInt().toString(), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Slider(
                    value = length, onValueChange = { length = it }, valueRange = 6f..32f, steps = 25,
                    colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary, inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant)
                )
                Spacer(modifier = Modifier.height(12.dp))
                PasswordGeneratorSwitch("大写字母 (A-Z)", includeUppercase) { includeUppercase = it }
                PasswordGeneratorSwitch("小写字母 (a-z)", includeLowercase) { includeLowercase = it }
                PasswordGeneratorSwitch("数字 (0-9)", includeNumbers) { includeNumbers = it }
                PasswordGeneratorSwitch("符号 (!@#...)", includeSymbols) { includeSymbols = it }
            }
        },
        confirmButton = { TextButton(onClick = { onGenerate(generatedPassword) }, enabled = generatedPassword.isNotEmpty()) { Text("使用密码", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
    )
}

@Composable
fun PasswordGeneratorSwitch(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        Switch(checked = checked, onCheckedChange = onCheckedChange, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = MaterialTheme.colorScheme.primary, uncheckedThumbColor = Color.White, uncheckedTrackColor = MaterialTheme.colorScheme.outline))
    }
}

@Composable
fun CategorySelectionField(
    selectedCategoryId: Long, categories: List<com.example.passvault.data.model.Category>,
    onCategorySelected: (Long) -> Unit, onAddCategory: ((String, Int) -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    val selectedCategory = categories.find { it.id == selectedCategoryId }

    Column(
        modifier = Modifier.fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.medium)
            .clickable { expanded = true }
            .padding(16.dp)
    ) {
        Text("分类", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (selectedCategory != null) {
                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(safeCategoryColor(selectedCategory.color)))
                Spacer(modifier = Modifier.width(8.dp))
                Text(selectedCategory.name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            } else {
                Text("选择分类", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, shape = MaterialTheme.shapes.medium, containerColor = MaterialTheme.colorScheme.surface) {
            categories.forEach { category ->
                DropdownMenuItem(
                    text = { Row(verticalAlignment = Alignment.CenterVertically) { Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(safeCategoryColor(category.color))); Spacer(Modifier.width(8.dp)); Text(category.name, style = MaterialTheme.typography.bodyMedium) } },
                    onClick = { onCategorySelected(category.id); expanded = false }
                )
            }
            if (categories.isNotEmpty()) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(vertical = 4.dp))
            DropdownMenuItem(
                text = { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(8.dp)); Text("添加新分类", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary) } },
                onClick = { showAddDialog = true; expanded = false }
            )
        }
    }
    if (showAddDialog && onAddCategory != null) {
        AddCategoryDialog(onDismiss = { showAddDialog = false }, onConfirm = { name, color -> onAddCategory(name, color); showAddDialog = false })
    }
}

class LimitedPasswordVisualTransformation(private val maxLength: Int = 12, private val mask: Char = '•') : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val originalLength = text.text.length
        val maskedLength = minOf(originalLength, maxLength)
        val maskedText = AnnotatedString(mask.toString().repeat(maskedLength))
        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int) = minOf(offset, maskedLength)
            override fun transformedToOriginal(offset: Int) = minOf(offset, originalLength)
        }
        return TransformedText(maskedText, offsetMapping)
    }
}
