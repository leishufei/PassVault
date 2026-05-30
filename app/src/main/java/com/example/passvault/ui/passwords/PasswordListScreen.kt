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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.passvault.data.model.Category
import com.example.passvault.data.repository.PasswordEntryListItem
import com.example.passvault.ui.theme.BrandColors
import com.example.passvault.ui.theme.CardBackground
import com.example.passvault.ui.theme.CardBorder
import com.example.passvault.ui.theme.DarkSurface
import com.example.passvault.ui.theme.PrimaryIndigo
import com.example.passvault.ui.theme.PrimaryPurple
import com.example.passvault.ui.theme.TextSecondary
import com.example.passvault.ui.theme.TextTertiary
import com.example.passvault.ui.theme.WarningYellow
import kotlin.math.abs

@Composable
fun PasswordListScreen(
    viewModel: PasswordViewModel,
    onPasswordClick: (Long) -> Unit,
    onAddClick: () -> Unit
) {
    val passwords by viewModel.passwords.collectAsState()
    val filterMode by viewModel.filterMode.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val categoryCounts by viewModel.categoryCounts.collectAsState()

    var isSearchActive by remember { mutableStateOf(false) }
    var isCategoryDropdownExpanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (isSearchActive) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.searchPasswords(it) },
                        placeholder = { Text("搜索密码...", color = TextTertiary) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(16.dp))
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White.copy(alpha = 0.07f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.07f),
                            focusedBorderColor = Color.White.copy(alpha = 0.1f),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "取消",
                        color = PrimaryPurple,
                        fontSize = 14.sp,
                        modifier = Modifier.clickable {
                            isSearchActive = false
                            viewModel.searchPasswords("")
                        }
                    )
                } else {
                    Text(
                        text = "密码",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.07f))
                                .clickable { isSearchActive = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Search, contentDescription = "Search", tint = Color(0xFFB0B0CC), modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            // Filter Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Category dropdown
                if (categories.isNotEmpty()) {
                    Box {
                        val currentCategoryLabel = when (filterMode) {
                            is PasswordFilter.CATEGORY -> {
                                val cat = categories.find { it.id == (filterMode as PasswordFilter.CATEGORY).categoryId }
                                cat?.name ?: categories.first().name
                            }
                            else -> categories.first().name
                        }

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(if (filterMode is PasswordFilter.CATEGORY) PrimaryPurple else Color.White.copy(alpha = 0.07f))
                                .clickable { isCategoryDropdownExpanded = true }
                                .padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp),
                                tint = if (filterMode is PasswordFilter.CATEGORY) Color.White else TextSecondary
                            )
                            Text(
                                text = currentCategoryLabel,
                                fontSize = 12.sp,
                                fontWeight = if (filterMode is PasswordFilter.CATEGORY) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (filterMode is PasswordFilter.CATEGORY) Color.White else TextSecondary
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (filterMode is PasswordFilter.CATEGORY) Color.White else TextSecondary
                            )
                        }

                        DropdownMenu(
                            expanded = isCategoryDropdownExpanded,
                            onDismissRequest = { isCategoryDropdownExpanded = false },
                            modifier = Modifier.background(DarkSurface, RoundedCornerShape(16.dp))
                        ) {
                            categories.forEach { category ->
                                val isSelected = filterMode is PasswordFilter.CATEGORY &&
                                        (filterMode as PasswordFilter.CATEGORY).categoryId == category.id
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .clip(CircleShape)
                                                    .background(BrandColors[category.color % BrandColors.size])
                                            )
                                            Text(
                                                "${category.name} (${categoryCounts[category.id] ?: 0})",
                                                color = if (isSelected) PrimaryPurple else Color.White,
                                                fontSize = 14.sp,
                                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                            )
                                        }
                                    },
                                    onClick = {
                                        viewModel.setFilter(PasswordFilter.CATEGORY(category.id))
                                        isCategoryDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                PillFilterChip(
                    text = "收藏",
                    icon = Icons.Default.StarBorder,
                    selected = filterMode == PasswordFilter.FAVORITES,
                    onClick = { viewModel.setFilter(PasswordFilter.FAVORITES) }
                )

                PillFilterChip(
                    text = "近期使用",
                    icon = Icons.Default.History,
                    selected = filterMode == PasswordFilter.RECENT,
                    onClick = { viewModel.setFilter(PasswordFilter.RECENT) }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Password List
            if (passwords.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("暂无密码记录", color = TextTertiary, fontSize = 14.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(passwords, key = { it.id }) { password ->
                        PasswordListItem(
                            password = password,
                            onClick = {
                                viewModel.markAsUsed(password.id)
                                onPasswordClick(password.id)
                            },
                            onFavoriteClick = {
                                viewModel.toggleFavorite(password.id, !password.isFavorite)
                            },
                            onCopyPassword = { onCopied ->
                                viewModel.decryptPassword(password.id) { decrypted ->
                                    onCopied(decrypted)
                                }
                            },
                            onDelete = {
                                viewModel.deletePassword(password.id)
                            }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }

        // FAB
        FloatingActionButton(
            onClick = onAddClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 24.dp, bottom = 24.dp)
                .size(40.dp),
            shape = CircleShape,
            containerColor = PrimaryPurple
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.White, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun PillFilterChip(
    text: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) PrimaryPurple else Color.White.copy(alpha = 0.07f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(13.dp),
            tint = if (selected) Color.White else TextSecondary
        )
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) Color.White else TextSecondary
        )
    }
}

@Composable
fun PasswordListItem(
    password: PasswordEntryListItem,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onCopyPassword: (onCopied: (String) -> Unit) -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current

    val colorIndex = if (password.iconColor != 0) password.iconColor % BrandColors.size else abs(password.title.hashCode()) % BrandColors.size
    val bgColor = BrandColors[colorIndex]

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBackground)
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(bgColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = password.title.take(1).uppercase(),
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Text
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = password.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = password.username,
                fontSize = 13.sp,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Actions
        IconButton(onClick = onFavoriteClick, modifier = Modifier.size(32.dp)) {
            Icon(
                imageVector = if (password.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                contentDescription = "Favorite",
                tint = if (password.isFavorite) WarningYellow else TextTertiary,
                modifier = Modifier.size(16.dp)
            )
        }

        Box {
            IconButton(onClick = { showMenu = true }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.MoreVert, contentDescription = "More", tint = TextTertiary, modifier = Modifier.size(16.dp))
            }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                modifier = Modifier.background(DarkSurface, RoundedCornerShape(16.dp))
            ) {
                DropdownMenuItem(
                    text = { Text("复制密码", color = Color.White, fontSize = 14.sp) },
                    onClick = {
                        onCopyPassword { decryptedPassword ->
                            clipboardManager.setText(AnnotatedString(decryptedPassword))
                        }
                        showMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("复制用户名", color = Color.White, fontSize = 14.sp) },
                    onClick = {
                        clipboardManager.setText(AnnotatedString(password.username))
                        showMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("删除", color = Color(0xFFEF4444), fontSize = 14.sp) },
                    onClick = {
                        onDelete()
                        showMenu = false
                    }
                )
            }
        }
    }
}

@Composable
fun CategoryFilterChip(
    category: com.example.passvault.data.model.Category,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) BrandColors[category.color % BrandColors.size] else Color.White.copy(alpha = 0.07f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (selected) Color.White else BrandColors[category.color % BrandColors.size])
        )
        Text(
            text = category.name,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) Color.White else TextSecondary
        )
    }
}
