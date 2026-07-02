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
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Deselect
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import android.widget.Toast
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.passvault.data.repository.PasswordEntryListItem
import com.example.passvault.ui.theme.CategoryConstants
import com.example.passvault.ui.theme.SemanticWarning
import com.example.passvault.ui.theme.safeCategoryColor
import kotlinx.coroutines.flow.drop
import kotlin.math.abs

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PasswordListScreen(
    viewModel: PasswordViewModel,
    onPasswordClick: (Long) -> Unit,
    onAddClick: () -> Unit
) {
    val passwords by viewModel.passwords.collectAsState()
    val selectedCategoryId by viewModel.selectedCategoryId.collectAsState()
    val isFavoritesOnly by viewModel.isFavoritesOnly.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isSearchActive by viewModel.isSearchActive.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val categoryCounts by viewModel.categoryCounts.collectAsState()
    val selectionMode by viewModel.selectionMode.collectAsState()
    val selectedIds by viewModel.selectedIds.collectAsState()

    val searchFocusRequester = remember { FocusRequester() }
    var isFilterDropdownExpanded by remember { mutableStateOf(false) }
    var isCategoryExpanded by remember { mutableStateOf(false) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var showBulkDeleteDialog by remember { mutableStateOf(false) }
    var singleDeleteTarget by remember { mutableStateOf<PasswordEntryListItem?>(null) }
    var showCategoryPicker by remember { mutableStateOf(false) }
    var singleEditTarget by remember { mutableStateOf<PasswordEntryListItem?>(null) }
    var isCategoryEditMode by remember { mutableStateOf(false) }
    var deleteCategoryTarget by remember { mutableStateOf<com.example.passvault.data.model.Category?>(null) }
    val listState = rememberSaveable(saver = LazyListState.Saver) {
        LazyListState()
    }

    val chipPositions = remember { mutableStateMapOf<Long, Rect>() }
    var draggingChipId by remember { mutableStateOf<Long?>(null) }
    var chipDragOffset by remember { mutableStateOf(Offset.Zero) }
    var workingCategoryOrder by remember { mutableStateOf<List<Long>>(emptyList()) }
    LaunchedEffect(categories, isCategoryEditMode) {
        val orderedFromDb = categories
            .filter { it.id != CategoryConstants.UNCATEGORIZED_ID }
            .map { it.id }
        if (!isCategoryEditMode || workingCategoryOrder.isEmpty()) {
            workingCategoryOrder = orderedFromDb
        } else {
            val currentSet = orderedFromDb.toSet()
            val preserved = workingCategoryOrder.filter { it in currentSet }
            val preservedSet = preserved.toSet()
            val added = orderedFromDb.filter { it !in preservedSet }
            workingCategoryOrder = preserved + added
        }
    }

    LaunchedEffect(Unit) {
        snapshotFlow { selectedCategoryId }
            .drop(1)
            .collect {
                if (passwords.isNotEmpty()) listState.scrollToItem(0)
            }
    }

    LaunchedEffect(Unit) {
        snapshotFlow { isFavoritesOnly }
            .drop(1)
            .collect {
                if (passwords.isNotEmpty()) listState.scrollToItem(0)
            }
    }

    LaunchedEffect(Unit) {
        snapshotFlow { sortOrder }
            .drop(1)
            .collect {
                if (passwords.isNotEmpty()) listState.scrollToItem(0)
            }
    }

    LaunchedEffect(Unit) {
        snapshotFlow { isSearchActive }
            .drop(1)
            .collect { active ->
                if (active && passwords.isNotEmpty()) listState.scrollToItem(0)
            }
    }

    BackHandler(enabled = isSearchActive && !selectionMode && !isCategoryEditMode) {
        viewModel.setSearchActive(false)
    }
    BackHandler(enabled = isCategoryEditMode && !selectionMode) {
        isCategoryEditMode = false
    }
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
                        totalCount = passwords.size,
                        onClose = { viewModel.exitSelectionMode() },
                        onToggleSelectAll = { viewModel.toggleSelectAll() },
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
                            LaunchedEffect(Unit) {
                                kotlinx.coroutines.delay(100)
                                try {
                                    searchFocusRequester.requestFocus()
                                } catch (_: IllegalStateException) {
                                    // FocusRequester not yet attached
                                }
                            }
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
                                            modifier = Modifier.fillMaxWidth().focusRequester(searchFocusRequester),
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
                                    viewModel.setSearchActive(false)
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
                                        .clickable { viewModel.setSearchActive(true) },
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
                                    val isFilterActive = isFavoritesOnly
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
                                            trailingIcon = if (isFavoritesOnly) {{
                                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                            }} else null,
                                            onClick = {
                                                viewModel.toggleFavorites()
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
                Column {
                    if (isCategoryEditMode) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "长按拖动调整顺序",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "完成",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .clickable { isCategoryEditMode = false }
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }
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
                            label = CategoryConstants.UNCATEGORIZED_NAME,
                            count = categoryCounts[0L] ?: 0,
                            color = CategoryConstants.UNCATEGORIZED_COLOR,
                            selected = selectedCategoryId == 0L,
                            editMode = isCategoryEditMode,
                            deletable = false,
                            onClick = {
                                if (!isCategoryEditMode) {
                                    viewModel.setCategory(0L)
                                }
                            },
                            onLongClick = { isCategoryEditMode = !isCategoryEditMode },
                            onDelete = {}
                        )
                        val categoryById = remember(categories) { categories.associateBy { it.id } }
                        val orderedCategories = if (isCategoryEditMode) {
                            workingCategoryOrder.mapNotNull { categoryById[it] }
                        } else {
                            categories.filter { it.id != CategoryConstants.UNCATEGORIZED_ID }
                        }
                        orderedCategories.forEach { category ->
                            val dragModifier = if (isCategoryEditMode) {
                                Modifier
                                    .onGloballyPositioned { coords ->
                                        if (draggingChipId != category.id) {
                                            chipPositions[category.id] = coords.boundsInWindow()
                                        }
                                    }
                                    .zIndex(if (draggingChipId == category.id) 10f else 0f)
                                    .offset {
                                        if (draggingChipId == category.id) {
                                            IntOffset(
                                                chipDragOffset.x.toInt(),
                                                chipDragOffset.y.toInt()
                                            )
                                        } else IntOffset.Zero
                                    }
                                    .pointerInput(category.id, isCategoryEditMode) {
                                        detectDragGesturesAfterLongPress(
                                            onDragStart = {
                                                draggingChipId = category.id
                                                chipDragOffset = Offset.Zero
                                            },
                                            onDrag = { _, delta ->
                                                chipDragOffset += delta
                                            },
                                            onDragEnd = {
                                                val sourceRect = chipPositions[category.id]
                                                val draggedId = draggingChipId
                                                if (sourceRect != null && draggedId != null) {
                                                    val draggedCenter = sourceRect.center + chipDragOffset
                                                    val targetEntry = chipPositions.entries
                                                        .firstOrNull { (id, rect) ->
                                                            id != draggedId && rect.contains(draggedCenter)
                                                        }
                                                    val targetId = targetEntry?.key
                                                    if (targetId != null) {
                                                        val current = workingCategoryOrder.toMutableList()
                                                        val from = current.indexOf(draggedId)
                                                        val to = current.indexOf(targetId)
                                                        if (from != -1 && to != -1 && from != to) {
                                                            current.removeAt(from)
                                                            current.add(to, draggedId)
                                                            workingCategoryOrder = current
                                                            viewModel.reorderCategories(current)
                                                        }
                                                    }
                                                }
                                                draggingChipId = null
                                                chipDragOffset = Offset.Zero
                                            },
                                            onDragCancel = {
                                                draggingChipId = null
                                                chipDragOffset = Offset.Zero
                                            }
                                        )
                                    }
                            } else Modifier
                            CategoryChip(
                                modifier = dragModifier,
                                label = category.name,
                                count = categoryCounts[category.id] ?: 0,
                                color = safeCategoryColor(category.color),
                                selected = selectedCategoryId == category.id,
                                editMode = isCategoryEditMode,
                                deletable = true,
                                onClick = {
                                    if (!isCategoryEditMode) {
                                        viewModel.setCategory(category.id)
                                    }
                                },
                                onLongClick = { isCategoryEditMode = !isCategoryEditMode },
                                onDelete = { deleteCategoryTarget = category }
                            )
                        }
                        AddCategoryChip(onClick = { showAddCategoryDialog = true })
                        if (isCategoryExpanded) {
                            MoreChip(label = "收起", onClick = { isCategoryExpanded = false })
                        }
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
                    state = listState,
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
                        val onClick = remember(password.id, selectionMode) {
                            {
                                if (selectionMode) {
                                    viewModel.toggleSelection(password.id)
                                } else {
                                    onPasswordClick(password.id)
                                }
                            }
                        }
                        val onLongClick = remember(password.id, selectionMode) {
                            {
                                if (!selectionMode) viewModel.enterSelectionMode(password.id)
                            }
                        }
                        val onFavoriteClick = remember(password.id, password.isFavorite) {
                            { viewModel.toggleFavorite(password.id, !password.isFavorite) }
                        }
                        val onCopyPassword = remember(password.id) {
                            { onCopied: (String) -> Unit ->
                                viewModel.decryptPassword(password.id) { decrypted ->
                                    onCopied(decrypted)
                                }
                            }
                        }
                        val onCopyUsername = remember(password.id) {
                            { }
                        }
                        val onMoveCategory = remember(password.id) {
                            {
                                singleEditTarget = password
                                showCategoryPicker = true
                            }
                        }
                        val onDelete = remember(password.id) {
                            { singleDeleteTarget = password }
                        }
                        PasswordListItem(
                            password = password,
                            selectionMode = selectionMode,
                            isSelected = isSelected,
                            modifier = Modifier.animateItem(),
                            onClick = onClick,
                            onLongClick = onLongClick,
                            onFavoriteClick = onFavoriteClick,
                            onCopyUsername = onCopyUsername,
                            onCopyPassword = onCopyPassword,
                            onMoveCategory = onMoveCategory,
                            onDelete = onDelete
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

    singleDeleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { singleDeleteTarget = null },
            shape = MaterialTheme.shapes.large,
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    "删除密码",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    "确定删除「${target.title}」吗？此操作无法撤销。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deletePassword(target.id)
                    singleDeleteTarget = null
                }) {
                    Text(
                        "删除",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { singleDeleteTarget = null }) {
                    Text(
                        "取消",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        )
    }

    deleteCategoryTarget?.let { target ->
        val passwordCount = categoryCounts[target.id] ?: 0
        AlertDialog(
            onDismissRequest = { deleteCategoryTarget = null },
            shape = MaterialTheme.shapes.large,
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    "删除分类",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                val message = if (passwordCount > 0) {
                    "确定删除分类「${target.name}」吗？该分类下的 $passwordCount 个密码也将被删除，此操作无法撤销。"
                } else {
                    "确定删除分类「${target.name}」吗？"
                }
                Text(
                    message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteCategory(target.id)
                    deleteCategoryTarget = null
                }) {
                    Text(
                        "删除",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteCategoryTarget = null }) {
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
    totalCount: Int,
    onClose: () -> Unit,
    onToggleSelectAll: () -> Unit,
    onMoveCategory: () -> Unit,
    onDelete: () -> Unit
) {
    val hasSelection = selectedCount > 0
    val isAllSelected = totalCount > 0 && selectedCount == totalCount
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
            icon = if (isAllSelected) Icons.Default.Deselect else Icons.Default.SelectAll,
            contentDescription = if (isAllSelected) "取消全选" else "全选",
            activeBackground = if (isAllSelected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant,
            activeTint = if (isAllSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            onClick = onToggleSelectAll
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CategoryChip(
    label: String,
    count: Int,
    color: Color?,
    selected: Boolean,
    editMode: Boolean = false,
    deletable: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    onDelete: () -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(
                    if (selected) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant
                )
                .combinedClickable(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onClick()
                    },
                    onLongClick = if (editMode) null else {
                        {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onLongClick()
                        }
                    }
                )
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
        if (editMode && deletable) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 4.dp, y = (-4).dp)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.error)
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onDelete()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Delete category",
                    tint = MaterialTheme.colorScheme.onError,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
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
    onCopyUsername: () -> Unit,
    onCopyPassword: (onCopied: (String) -> Unit) -> Unit,
    onMoveCategory: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val bgColor = remember(password.iconColor, password.title) {
        safeCategoryColor(
            if (password.iconColor != 0) password.iconColor else abs(password.title.hashCode())
        )
    }
    val avatarLetter = remember(password.title) { password.title.take(1).uppercase() }

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
                            text = avatarLetter,
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
                                Toast.makeText(context, "已复制用户名", Toast.LENGTH_SHORT).show()
                                onCopyUsername()
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
                                    Toast.makeText(context, "已复制密码", Toast.LENGTH_SHORT).show()
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
