package com.example.passvault.ui.passwords

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowOverflow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.passvault.data.repository.PasswordEntryListItem
import com.example.passvault.ui.theme.SemanticWarning
import com.example.passvault.ui.theme.safeCategoryColor
import kotlin.math.abs

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PasswordListScreen(
    viewModel: PasswordViewModel,
    onPasswordClick: (Long) -> Unit,
    onAddClick: () -> Unit
) {
    val passwords by viewModel.passwords.collectAsState()
    val filterMode by viewModel.filterMode.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val categoryCounts by viewModel.categoryCounts.collectAsState()
    val selectionMode by viewModel.selectionMode.collectAsState()
    val selectedIds by viewModel.selectedIds.collectAsState()

    var isSearchActive by remember { mutableStateOf(false) }
    var isFilterDropdownExpanded by remember { mutableStateOf(false) }
    var isCategoryExpanded by remember { mutableStateOf(false) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var showBulkDeleteDialog by remember { mutableStateOf(false) }
    var showCategoryPicker by remember { mutableStateOf(false) }
    var singleEditTarget by remember { mutableStateOf<PasswordEntryListItem?>(null) }

    BackHandler(enabled = selectionMode) { viewModel.exitSelectionMode() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Crossfade(
                targetState = selectionMode,
                animationSpec = tween(120),
                label = "topbar"
            ) { inSelection ->
                if (inSelection) {
                    SelectionTopBar(
                        selectedCount = selectedIds.size,
                        onClose = { viewModel.exitSelectionMode() },
                        onSelectAll = { viewModel.selectAllVisible() },
                        onMoveCategory = {
                            singleEditTarget = null
                            showCategoryPicker = true
                        },
                        onDelete = { showBulkDeleteDialog = true }
                    )
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        if (isSearchActive) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(horizontal = 12.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Search,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(modifier = Modifier.weight(1f)) {
                                        BasicTextField(
                                            value = searchQuery,
                                            onValueChange = { viewModel.searchPasswords(it) },
                                            modifier = Modifier.fillMaxWidth(),
                                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                                color = MaterialTheme.colorScheme.onSurface
                                            ),
                                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                            singleLine = true,
                                            decorationBox = { innerTextField ->
                                                if (searchQuery.isEmpty()) {
                                                    Text(
                                                        "搜索密码...",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                innerTextField()
                                            }
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "取消",
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.clickable {
                                    isSearchActive = false
                                    viewModel.searchPasswords("")
                                }
                            )
                        } else {
                            Text(
                                text = "密码",
                                style = MaterialTheme.typography.headlineMedium
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable { isSearchActive = true },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Search,
                                        contentDescription = "Search",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Box {
                                    val isFilterActive = filterMode == PasswordFilter.FAVORITES
                                        || filterMode == PasswordFilter.RECENT
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isFilterActive) MaterialTheme.colorScheme.primaryContainer
                                                else MaterialTheme.colorScheme.surfaceVariant
                                            )
                                            .clickable { isFilterDropdownExpanded = true },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.FilterList,
                                            contentDescription = "Filter",
                                            tint = if (isFilterActive) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    DropdownMenu(
                                        expanded = isFilterDropdownExpanded,
                                        onDismissRequest = { isFilterDropdownExpanded = false },
                                        shape = MaterialTheme.shapes.medium,
                                        containerColor = MaterialTheme.colorScheme.surface
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("收藏", style = MaterialTheme.typography.bodyMedium) },
                                            leadingIcon = {
                                                Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(18.dp))
                                            },
                                            trailingIcon = if (filterMode == PasswordFilter.FAVORITES) {{
                                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                            }} else null,
                                            onClick = {
                                                viewModel.setFilter(PasswordFilter.FAVORITES)
                                                isFilterDropdownExpanded = false
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("近期使用", style = MaterialTheme.typography.bodyMedium) },
                                            leadingIcon = {
                                                Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp))
                                            },
                                            trailingIcon = if (filterMode == PasswordFilter.RECENT) {{
                                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                            }} else null,
                                            onClick = {
                                                viewModel.setFilter(PasswordFilter.RECENT)
                                                isFilterDropdownExpanded = false
                                            }
                                        )
                                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                        DropdownMenuItem(
                                            text = { Text("创建时间 (新→旧)", style = MaterialTheme.typography.bodyMedium) },
                                            trailingIcon = if (sortOrder == SortOrder.CREATED_DESC) {{
                                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                            }} else null,
                                            onClick = {
                                                viewModel.setSortOrder(SortOrder.CREATED_DESC)
                                                isFilterDropdownExpanded = false
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("创建时间 (旧→新)", style = MaterialTheme.typography.bodyMedium) },
                                            trailingIcon = if (sortOrder == SortOrder.CREATED_ASC) {{
                                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                            }} else null,
                                            onClick = {
                                                viewModel.setSortOrder(SortOrder.CREATED_ASC)
                                                isFilterDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(visible = !selectionMode) {
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    maxLines = if (isCategoryExpanded) Int.MAX_VALUE else 2,
                    overflow = FlowRowOverflow.expandIndicator {
                        MoreChip(label = "更多", onClick = { isCategoryExpanded = true })
                    }
                ) {
                    CategoryChip(
                        label = "全部",
                        count = categoryCounts[0L] ?: 0,
                        color = null,
                        selected = filterMode is PasswordFilter.CATEGORY
                            && (filterMode as PasswordFilter.CATEGORY).categoryId == 0L,
                        onClick = { viewModel.setFilter(PasswordFilter.CATEGORY(0L)) }
                    )
                    categories.forEach { category ->
                        CategoryChip(
                            label = category.name,
                            count = categoryCounts[category.id] ?: 0,
                            color = safeCategoryColor(category.color),
                            selected = filterMode is PasswordFilter.CATEGORY
                                && (filterMode as PasswordFilter.CATEGORY).categoryId == category.id,
                            onClick = { viewModel.setFilter(PasswordFilter.CATEGORY(category.id)) }
                        )
                    }
                    AddCategoryChip(onClick = { showAddCategoryDialog = true })
                    if (isCategoryExpanded) {
                        MoreChip(label = "收起", onClick = { isCategoryExpanded = false })
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (passwords.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "暂无密码记录",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(
                        items = passwords,
                        key = { it.id },
                        contentType = { "PasswordRow" }
                    ) { password ->
                        val isSelected = password.id in selectedIds
                        PasswordListItem(
                            password = password,
                            selectionMode = selectionMode,
                            isSelected = isSelected,
                            modifier = Modifier.animateItem(),
                            onClick = {
                                if (selectionMode) {
                                    viewModel.toggleSelection(password.id)
                                } else {
                                    viewModel.markAsUsed(password.id)
                                    onPasswordClick(password.id)
                                }
                            },
                            onLongClick = {
                                if (!selectionMode) viewModel.enterSelectionMode(password.id)
                            },
                            onFavoriteClick = {
                                viewModel.toggleFavorite(password.id, !password.isFavorite)
                            },
                            onCopyPassword = { onCopied ->
                                viewModel.decryptPassword(password.id) { decrypted ->
                                    onCopied(decrypted)
                                }
                            },
                            onMoveCategory = {
                                singleEditTarget = password
                                showCategoryPicker = true
                            },
                            onDelete = {
                                viewModel.deletePassword(password.id)
                            }
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = !selectionMode,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 24.dp, bottom = 24.dp)
        ) {
            FloatingActionButton(
                onClick = onAddClick,
                modifier = Modifier.size(56.dp),
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }

    if (showAddCategoryDialog) {
        AddCategoryDialog(
            onDismiss = { showAddCategoryDialog = false },
            onConfirm = { name, color ->
                viewModel.addCategory(name, color)
                showAddCategoryDialog = false
            }
        )
    }

    if (showCategoryPicker) {
        val target = singleEditTarget
        CategoryPickerSheet(
            categories = categories,
            currentCategoryId = target?.categoryId,
            title = if (target == null) "移动 ${selectedIds.size} 项到分类" else "移动到分类",
            onPick = { newId ->
                if (target == null) viewModel.bulkChangeCategory(newId)
                else viewModel.changeCategory(target.id, newId)
            },
            onDismiss = {
                showCategoryPicker = false
                singleEditTarget = null
            },
            onAddCategory = { name, colorIndex ->
                viewModel.addCategory(name, colorIndex)
            }
        )
    }

    if (showBulkDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showBulkDeleteDialog = false },
            shape = MaterialTheme.shapes.large,
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    "删除所选项",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    "确定删除选中的 ${selectedIds.size} 个密码吗？",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.bulkDelete()
                    showBulkDeleteDialog = false
                }) {
                    Text(
                        "删除",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showBulkDeleteDialog = false }) {
                    Text(
                        "取消",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        )
    }
}

@Composable
private fun SelectionTopBar(
    selectedCount: Int,
    onClose: () -> Unit,
    onSelectAll: () -> Unit,
    onMoveCategory: () -> Unit,
    onDelete: () -> Unit
) {
    val hasSelection = selectedCount > 0
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TopBarIconButton(
            icon = Icons.Default.Close,
            contentDescription = "退出多选",
            onClick = onClose
        )
        Text(
            text = "已选 $selectedCount 项",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        TopBarIconButton(
            icon = Icons.Default.SelectAll,
            contentDescription = "全选",
            onClick = onSelectAll
        )
        TopBarIconButton(
            icon = Icons.Default.DriveFileMove,
            contentDescription = "移动到分类",
            enabled = hasSelection,
            activeBackground = MaterialTheme.colorScheme.primaryContainer,
            activeTint = MaterialTheme.colorScheme.primary,
            onClick = onMoveCategory
        )
        TopBarIconButton(
            icon = Icons.Default.Delete,
            contentDescription = "删除",
            enabled = hasSelection,
            activeBackground = MaterialTheme.colorScheme.surfaceVariant,
            activeTint = MaterialTheme.colorScheme.error,
            onClick = onDelete
        )
    }
}

@Composable
private fun TopBarIconButton(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean = true,
    activeBackground: Color = MaterialTheme.colorScheme.surfaceVariant,
    activeTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    onClick: () -> Unit
) {
    val background = if (enabled) activeBackground else MaterialTheme.colorScheme.surfaceVariant
    val tint = if (enabled) activeTint
        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(background)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
fun CategoryChip(
    label: String,
    count: Int,
    color: Color?,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (color != null) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }
        Text(
            text = "$label ($count)",
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun MoreChip(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun AddCategoryChip(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            Icons.Default.Add,
            contentDescription = "Add category",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = "新建",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PasswordListItem(
    password: PasswordEntryListItem,
    selectionMode: Boolean,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onCopyPassword: (onCopied: (String) -> Unit) -> Unit,
    onMoveCategory: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current
    val bgColor = safeCategoryColor(
        if (password.iconColor != 0) password.iconColor else abs(password.title.hashCode())
    )

    val containerColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
            else MaterialTheme.colorScheme.surface,
        animationSpec = tween(120),
        label = "containerColor"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outlineVariant,
        animationSpec = tween(120),
        label = "borderColor"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(containerColor)
            .border(1.dp, borderColor, MaterialTheme.shapes.medium)
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                }
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Crossfade(
            targetState = selectionMode,
            animationSpec = tween(120),
            label = "leading"
        ) { inSelection ->
            Box(
                modifier = Modifier.size(40.dp),
                contentAlignment = Alignment.Center
            ) {
                if (inSelection) {
                    Icon(
                        imageVector = if (isSelected) Icons.Default.CheckCircle
                            else Icons.Default.RadioButtonUnchecked,
                        contentDescription = if (isSelected) "已选中" else "未选中",
                        tint = if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(bgColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = password.title.take(1).uppercase(),
                            color = Color.White,
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = password.title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = password.username,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        AnimatedVisibility(
            visible = !selectionMode,
            enter = fadeIn(tween(120)),
            exit = fadeOut(tween(120))
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onFavoriteClick, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = if (password.isFavorite) Icons.Default.Star
                            else Icons.Default.StarBorder,
                        contentDescription = "Favorite",
                        tint = if (password.isFavorite) SemanticWarning
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Box {
                    IconButton(onClick = { showMenu = true }, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "More",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        shape = MaterialTheme.shapes.medium,
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.ContentCopy,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        "复制用户名",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            },
                            onClick = {
                                clipboardManager.setText(AnnotatedString(password.username))
                                showMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.ContentCopy,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        "复制密码",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            },
                            onClick = {
                                onCopyPassword { decryptedPassword ->
                                    clipboardManager.setText(AnnotatedString(decryptedPassword))
                                }
                                showMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.DriveFileMove,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        "更改分类",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            },
                            onClick = {
                                onMoveCategory()
                                showMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        "删除",
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            },
                            onClick = {
                                onDelete()
                                showMenu = false
                            }
                        )
                    }
                }
            }
        }
    }
}
