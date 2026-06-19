package com.example.passvault.ui.settings

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.passvault.domain.csv.CsvFormat
import com.example.passvault.domain.csv.CsvValidationResult
import com.example.passvault.domain.csv.FieldDiff
import com.example.passvault.domain.csv.ImportAction
import com.example.passvault.domain.csv.InvalidRow
import com.example.passvault.domain.csv.ValidatedPasswordRow

@Composable
fun ImportPreviewDialog(
    result: CsvValidationResult,
    onConfirm: (List<ValidatedPasswordRow>) -> Unit,
    onDismiss: () -> Unit
) {
    val selectedRows = remember {
        mutableStateOf(result.validRows.associate { it.rowIndex to true })
    }

    val selectedValidRows = remember(selectedRows.value) {
        result.validRows.filter { selectedRows.value[it.rowIndex] == true }
    }

    val displayedRows = remember {
        result.validRows.filter { row ->
            row.action == ImportAction.INSERT || row.diffs.isNotEmpty()
        }
    }

    val selectedDisplayedRows = remember(selectedRows.value) {
        selectedValidRows.filter { row ->
            row.action == ImportAction.INSERT || row.diffs.isNotEmpty()
        }
    }

    val selectedInsertCount = remember(selectedDisplayedRows) {
        selectedDisplayedRows.count { it.action == ImportAction.INSERT }
    }

    val selectedUpdateCount = remember(selectedDisplayedRows) {
        selectedDisplayedRows.count { it.action == ImportAction.UPDATE && it.diffs.isNotEmpty() }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        BackHandler { onDismiss() }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                PreviewHeader(onDismiss = onDismiss)

                SummaryCard(
                    result = result,
                    selectedValidCount = selectedDisplayedRows.size,
                    selectedInsertCount = selectedInsertCount,
                    selectedUpdateCount = selectedUpdateCount
                )
                Spacer(modifier = Modifier.height(12.dp))

                SelectionActionBar(
                    totalValid = displayedRows.size,
                    selectedCount = selectedDisplayedRows.size,
                    onSelectAll = {
                        selectedRows.value = result.validRows.associate { it.rowIndex to true }
                    },
                    onDeselectAll = {
                        selectedRows.value = result.validRows.associate { it.rowIndex to false }
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (result.invalidRows.isNotEmpty()) {
                        item {
                            SectionLabel("无效记录 (将跳过)")
                        }
                        items(result.invalidRows) { row ->
                            InvalidRowItem(row)
                        }
                    }
                    if (result.validRows.isNotEmpty()) {
                        item {
                            SectionLabel("待导入记录")
                        }
                        items(result.validRows.filter { row ->
                            row.action == ImportAction.INSERT || row.diffs.isNotEmpty()
                        }) { row ->
                            ValidRowItem(
                                row = row,
                                isSelected = selectedRows.value[row.rowIndex] == true,
                                onSelectionChange = { isSelected ->
                                    selectedRows.value = selectedRows.value.toMutableMap().apply {
                                        put(row.rowIndex, isSelected)
                                    }
                                }
                            )
                        }
                    }
                    item { Spacer(modifier = Modifier.height(100.dp)) }
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 56.dp)
            ) {
                Button(
                    onClick = { onConfirm(selectedValidRows) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    enabled = selectedDisplayedRows.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = "确认导入 (${selectedDisplayedRows.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun PreviewHeader(onDismiss: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "返回",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.size(12.dp))
        Text(
            text = "导入预览",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier
                .clip(MaterialTheme.shapes.medium)
                .clickable(onClick = onDismiss)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = "取消",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun SummaryCard(
    result: CsvValidationResult,
    selectedValidCount: Int,
    selectedInsertCount: Int,
    selectedUpdateCount: Int
) {
    val formatLabel = when (result.format) {
        CsvFormat.EXTENDED -> "扩展格式 (7列)"
        CsvFormat.LEGACY -> "旧格式 (4列)"
        CsvFormat.UNKNOWN -> "未知格式"
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.medium)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "导入摘要",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        StatLine(label = "格式", value = formatLabel)
        StatLine(label = "总计", value = "${result.totalRows} 条 (已选: $selectedValidCount 条)")
        StatLine(label = "更新现有", value = "${result.updateCount} 条 (已选: $selectedUpdateCount 条)")
        StatLine(label = "新增", value = "${result.insertCount} 条 (已选: $selectedInsertCount 条)")
        StatLine(
            label = "无效",
            value = "${result.invalidRows.size} 条",
            valueColor = if (result.invalidRows.isNotEmpty())
                MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
        if (result.newCategoryNames.isNotEmpty()) {
            StatLine(label = "新增分类", value = result.newCategoryNames.joinToString("、"))
        }
    }
}

@Composable
private fun StatLine(label: String, value: String, valueColor: Color = MaterialTheme.colorScheme.onSurface) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = valueColor,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}

@Composable
private fun SelectionActionBar(
    totalValid: Int,
    selectedCount: Int,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(
            onClick = onSelectAll,
            modifier = Modifier.weight(1f),
            enabled = selectedCount < totalValid
        ) {
            Text("全选")
        }
        OutlinedButton(
            onClick = onDeselectAll,
            modifier = Modifier.weight(1f),
            enabled = selectedCount > 0
        ) {
            Text("全不选")
        }
    }
}

@Composable
private fun ValidRowItem(
    row: ValidatedPasswordRow,
    isSelected: Boolean,
    onSelectionChange: (Boolean) -> Unit
) {
    val isUpdate = row.action == ImportAction.UPDATE
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.medium)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = onSelectionChange,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.size(8.dp))
            Icon(
                imageVector = if (isUpdate) Icons.Default.Edit else Icons.Default.Add,
                contentDescription = null,
                tint = if (isUpdate) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = row.title.ifEmpty { "(无标题)" },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = if (isUpdate) "更新" else "新增",
                style = MaterialTheme.typography.labelSmall,
                color = if (isUpdate) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.tertiary,
                fontWeight = FontWeight.SemiBold
            )
        }

        if (row.username.isNotEmpty()) {
            Text(
                text = row.username,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (isUpdate && row.diffs.isNotEmpty()) {
            Spacer(modifier = Modifier.height(2.dp))
            row.diffs.forEach { diff ->
                DiffLine(diff)
            }
        } else if (isUpdate && row.diffs.isEmpty()) {
            Text(
                text = "无字段变化",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            val tags = buildList {
                if (row.website.isNotEmpty()) add("网站: ${row.website}")
                if (row.notes.isNotEmpty()) add("备注: ${row.notes.take(20)}")
                if (row.categoryName.isNotEmpty()) add("分类: ${row.categoryName}")
            }
            if (tags.isNotEmpty()) {
                Text(
                    text = tags.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DiffLine(diff: FieldDiff) {
    val oldText = diff.oldValue.ifEmpty { "(空)" }
    val newText = diff.newValue.ifEmpty { "(空)" }
    Text(
        text = "• ${diff.field}: $oldText → $newText",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
private fun InvalidRowItem(row: InvalidRow) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f))
            .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f), MaterialTheme.shapes.medium)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.size(6.dp))
            Text(
                text = "第 ${row.rowIndex} 行",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.error
            )
        }
        Text(
            text = row.errorMessage,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
