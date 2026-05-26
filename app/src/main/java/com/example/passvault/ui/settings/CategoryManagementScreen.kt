package com.example.passvault.ui.settings

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.passvault.data.model.Category
import com.example.passvault.ui.theme.BrandColors
import com.example.passvault.ui.theme.CardBackground
import com.example.passvault.ui.theme.CardBorder
import com.example.passvault.ui.theme.DarkSurface
import com.example.passvault.ui.theme.PrimaryIndigo
import com.example.passvault.ui.theme.PrimaryPurple
import com.example.passvault.ui.theme.TextSecondary
import com.example.passvault.ui.theme.TextTertiary

@Composable
fun CategoryManagementScreen(
    viewModel: CategoryViewModel,
    onNavigateBack: () -> Unit
) {
    val categories by viewModel.categories.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf<Category?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
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
                        .clickable { onNavigateBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(0xFFB0B0CC),
                        modifier = Modifier.size(18.dp)
                    )
                }

                Text(
                    text = "分类管理",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(PrimaryPurple)
                        .clickable { showAddDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { Spacer(modifier = Modifier.height(8.dp)) }
                items(categories) { category ->
                    CategoryItem(
                        category = category,
                        onEdit = {
                            selectedCategory = category
                            showEditDialog = true
                        },
                        onDelete = {
                            selectedCategory = category
                            showDeleteDialog = true
                        }
                    )
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }

    if (showAddDialog) {
        CategoryEditDialog(
            title = "添加分类",
            initialName = "",
            initialColor = 0,
            onDismiss = { showAddDialog = false },
            onConfirm = { name, color ->
                viewModel.addCategory(name, color)
                showAddDialog = false
            }
        )
    }

    if (showEditDialog && selectedCategory != null) {
        CategoryEditDialog(
            title = "编辑分类",
            initialName = selectedCategory!!.name,
            initialColor = selectedCategory!!.color,
            onDismiss = { showEditDialog = false },
            onConfirm = { name, color ->
                viewModel.updateCategory(selectedCategory!!.id, name, color)
                showEditDialog = false
            }
        )
    }

    if (showDeleteDialog && selectedCategory != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = DarkSurface,
            shape = RoundedCornerShape(24.dp),
            title = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEF4444).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "删除 ${selectedCategory!!.name}?",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            },
            text = {
                Text(
                    if (selectedCategory!!.isDefault)
                        "预设分类不能删除"
                    else
                        "此操作不可撤销，该分类下的密码将移动到\"其他\"分类。",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                if (!selectedCategory!!.isDefault) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TextButton(
                            onClick = { showDeleteDialog = false },
                            modifier = Modifier.weight(1f).height(44.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.textButtonColors(
                                containerColor = Color.White.copy(alpha = 0.08f)
                            )
                        ) {
                            Text("取消", color = Color(0xFFD0D0F0), fontSize = 15.sp)
                        }
                        TextButton(
                            onClick = {
                                viewModel.deleteCategory(selectedCategory!!.id)
                                showDeleteDialog = false
                            },
                            modifier = Modifier.weight(1f).height(44.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.textButtonColors(
                                containerColor = Color(0xFFEF4444)
                            )
                        ) {
                            Text(
                                "删除",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                } else {
                    TextButton(
                        onClick = { showDeleteDialog = false },
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.textButtonColors(
                            containerColor = PrimaryPurple
                        )
                    ) {
                        Text("确定", color = Color.White, fontSize = 15.sp)
                    }
                }
            }
        )
    }
}

@Composable
fun CategoryItem(
    category: Category,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBackground)
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(BrandColors[category.color % BrandColors.size]),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = category.name,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
            if (category.isDefault) {
                Text(
                    text = "预设分类",
                    fontSize = 12.sp,
                    color = TextTertiary
                )
            }
        }

        IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
            Icon(
                Icons.Default.Edit,
                contentDescription = "Edit",
                tint = TextSecondary,
                modifier = Modifier.size(16.dp)
            )
        }

        if (!category.isDefault) {
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun CategoryEditDialog(
    title: String,
    initialName: String,
    initialColor: Int,
    onDismiss: () -> Unit,
    onConfirm: (String, Int) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var selectedColor by remember { mutableIntStateOf(initialColor) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("分类名称", color = TextSecondary) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White.copy(alpha = 0.07f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.07f),
                        focusedBorderColor = PrimaryPurple,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Column {
                    Text(
                        "选择颜色",
                        fontSize = 14.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(BrandColors.size) { index ->
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(BrandColors[index])
                                    .border(
                                        width = if (selectedColor == index) 3.dp else 0.dp,
                                        color = Color.White,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedColor = index },
                                contentAlignment = Alignment.Center
                            ) {
                                if (selectedColor == index) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.textButtonColors(
                        containerColor = Color.White.copy(alpha = 0.08f)
                    )
                ) {
                    Text("取消", color = Color(0xFFD0D0F0), fontSize = 15.sp)
                }
                Button(
                    onClick = { onConfirm(name, selectedColor) },
                    enabled = name.isNotBlank(),
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        disabledContainerColor = Color.White.copy(alpha = 0.08f)
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = if (name.isNotBlank())
                                    Brush.linearGradient(listOf(PrimaryPurple, PrimaryIndigo))
                                else
                                    Brush.linearGradient(listOf(Color.Transparent, Color.Transparent)),
                                shape = RoundedCornerShape(16.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "确定",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (name.isNotBlank()) Color.White else TextTertiary
                        )
                    }
                }
            }
        }
    )
}





