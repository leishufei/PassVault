package com.example.passvault.domain.csv

import com.example.passvault.data.model.Category
import com.example.passvault.data.repository.PasswordEntryDisplay

/**
 * Pure logic for validating CSV records and matching them against existing data.
 * Stateless — all dependencies passed in as parameters so it can be unit tested.
 */
class CsvValidator {

    companion object {
        const val UNCATEGORIZED = "未分类"
        const val EXTENDED_COLUMN_COUNT = 7
        const val LEGACY_COLUMN_COUNT = 4

        // Headers used when exporting in extended format.
        val EXTENDED_HEADERS = listOf("ID", "分类", "标题", "用户名", "密码", "网站", "备注")

        // Legacy export header (kept for backward compatibility).
        val LEGACY_HEADERS = listOf("分类", "名称", "用户名", "密码")

        private val UUID_PATTERN = Regex(
            "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}",
            RegexOption.IGNORE_CASE
        )
    }

    fun detectFormat(records: List<List<String>>): CsvFormat {
        if (records.isEmpty()) return CsvFormat.UNKNOWN
        val firstRow = records[0]

        if (firstRow.size >= EXTENDED_COLUMN_COUNT) {
            val firstCell = firstRow.getOrNull(0)?.trim().orEmpty()
            val hasIdHeader = firstCell.equals("ID", ignoreCase = true)
            val looksLikeUuid = UUID_PATTERN.matches(firstCell)
            val hasExtendedHeader = firstRow.getOrNull(2)?.contains("标题") == true ||
                firstRow.getOrNull(5)?.contains("网站") == true
            if (hasIdHeader || looksLikeUuid || hasExtendedHeader) {
                return CsvFormat.EXTENDED
            }
        }

        if (firstRow.size in LEGACY_COLUMN_COUNT until EXTENDED_COLUMN_COUNT) {
            return CsvFormat.LEGACY
        }

        return CsvFormat.UNKNOWN
    }

    fun validate(
        records: List<List<String>>,
        existingPasswords: List<PasswordEntryDisplay>,
        existingCategories: List<Category>
    ): CsvValidationResult {
        val sanitizedRecords = if (records.isNotEmpty() && records[0].isNotEmpty()) {
            val firstCell = records[0][0]
            if (firstCell.startsWith('﻿')) {
                records.toMutableList().apply {
                    this[0] = this[0].toMutableList().apply {
                        this[0] = firstCell.removePrefix("﻿")
                    }
                }
            } else {
                records
            }
        } else {
            records
        }

        val format = detectFormat(sanitizedRecords)
        if (format == CsvFormat.UNKNOWN || sanitizedRecords.isEmpty()) {
            return CsvValidationResult(
                format = format,
                totalRows = 0,
                validRows = emptyList(),
                invalidRows = emptyList(),
                newCategoryNames = emptyList()
            )
        }

        val startIdx = if (isHeaderRow(sanitizedRecords[0], format)) 1 else 0
        val validRows = mutableListOf<ValidatedPasswordRow>()
        val invalidRows = mutableListOf<InvalidRow>()
        val newCategoryNames = mutableSetOf<String>()
        val existingCategoryNamesLower = existingCategories.map { it.name.lowercase() }.toSet()
        val byUuid = existingPasswords.filter { it.uuid.isNotEmpty() }.associateBy { it.uuid }
        val byTitleUser = existingPasswords.associateBy {
            "${it.title.lowercase()}|${it.username.lowercase()}"
        }

        for (idx in startIdx until sanitizedRecords.size) {
            val record = sanitizedRecords[idx]
            val displayRow = idx + 1
            val parsed = parseRow(record, format)
            if (parsed == null) {
                invalidRows.add(InvalidRow(displayRow, record, "字段数量不足"))
                continue
            }
            if (parsed.title.isEmpty()) {
                invalidRows.add(InvalidRow(displayRow, record, "标题不能为空"))
                continue
            }
            if (parsed.username.isEmpty() && parsed.password.isEmpty()) {
                invalidRows.add(InvalidRow(displayRow, record, "用户名和密码不能同时为空"))
                continue
            }

            val existing = when (format) {
                CsvFormat.EXTENDED -> parsed.uuid?.takeIf { it.isNotEmpty() }?.let { byUuid[it] }
                CsvFormat.LEGACY -> byTitleUser["${parsed.title.lowercase()}|${parsed.username.lowercase()}"]
                CsvFormat.UNKNOWN -> null
            }

            val categoryNameNormalized = parsed.categoryName.ifEmpty { UNCATEGORIZED }
            if (categoryNameNormalized != UNCATEGORIZED &&
                !existingCategoryNamesLower.contains(categoryNameNormalized.lowercase())
            ) {
                newCategoryNames.add(categoryNameNormalized)
            }

            val row = if (existing != null) {
                ValidatedPasswordRow(
                    rowIndex = displayRow,
                    action = ImportAction.UPDATE,
                    title = parsed.title,
                    username = parsed.username,
                    password = parsed.password,
                    website = parsed.website,
                    notes = parsed.notes,
                    categoryName = categoryNameNormalized,
                    existingId = existing.id,
                    existingUuid = existing.uuid,
                    diffs = computeDiffs(existing, parsed, categoryNameNormalized, existingCategories)
                )
            } else {
                ValidatedPasswordRow(
                    rowIndex = displayRow,
                    action = ImportAction.INSERT,
                    title = parsed.title,
                    username = parsed.username,
                    password = parsed.password,
                    website = parsed.website,
                    notes = parsed.notes,
                    categoryName = categoryNameNormalized
                )
            }
            validRows.add(row)
        }

        return CsvValidationResult(
            format = format,
            totalRows = sanitizedRecords.size - startIdx,
            validRows = validRows,
            invalidRows = invalidRows,
            newCategoryNames = newCategoryNames.toList()
        )
    }

    private fun isHeaderRow(row: List<String>, format: CsvFormat): Boolean {
        return when (format) {
            CsvFormat.EXTENDED -> {
                val firstCell = row.getOrNull(0)?.trim().orEmpty()
                firstCell.equals("ID", ignoreCase = true) ||
                    row.getOrNull(2)?.contains("标题") == true
            }
            CsvFormat.LEGACY -> {
                val firstCell = row.getOrNull(0)?.trim().orEmpty()
                val secondCell = row.getOrNull(1)?.trim().orEmpty()
                firstCell.contains("分类") || secondCell.contains("名称")
            }
            CsvFormat.UNKNOWN -> false
        }
    }

    private fun parseRow(record: List<String>, format: CsvFormat): ParsedRow? {
        return when (format) {
            CsvFormat.EXTENDED -> {
                if (record.size < EXTENDED_COLUMN_COUNT) return null
                ParsedRow(
                    uuid = record[0].trim().takeIf { UUID_PATTERN.matches(it) },
                    categoryName = record[1].trim(),
                    title = record[2].trim(),
                    username = record[3].trim(),
                    password = record[4].trim(),
                    website = record[5].trim(),
                    notes = record[6].trim()
                )
            }
            CsvFormat.LEGACY -> {
                if (record.size < LEGACY_COLUMN_COUNT) return null
                ParsedRow(
                    uuid = null,
                    categoryName = record[0].trim(),
                    title = record[1].trim(),
                    username = record[2].trim(),
                    password = record[3].trim(),
                    website = "",
                    notes = ""
                )
            }
            CsvFormat.UNKNOWN -> null
        }
    }

    private fun computeDiffs(
        existing: PasswordEntryDisplay,
        parsed: ParsedRow,
        newCategoryName: String,
        categories: List<Category>
    ): List<FieldDiff> {
        val diffs = mutableListOf<FieldDiff>()
        if (existing.title != parsed.title) {
            diffs.add(FieldDiff("标题", existing.title, parsed.title))
        }
        if (existing.username != parsed.username) {
            diffs.add(FieldDiff("用户名", existing.username, parsed.username))
        }
        if (existing.password != parsed.password) {
            diffs.add(FieldDiff("密码", existing.password, parsed.password))
        }
        if (existing.website != parsed.website) {
            diffs.add(FieldDiff("网站", existing.website, parsed.website))
        }
        if (existing.notes != parsed.notes) {
            diffs.add(FieldDiff("备注", existing.notes, parsed.notes))
        }
        val existingCategoryName = categories.firstOrNull { it.id == existing.categoryId }?.name
            ?: UNCATEGORIZED
        if (existingCategoryName != newCategoryName) {
            diffs.add(FieldDiff("分类", existingCategoryName, newCategoryName))
        }
        return diffs
    }

    private fun maskPassword(value: String): String {
        if (value.isEmpty()) return ""
        return "*".repeat(value.length.coerceAtMost(8))
    }

    private data class ParsedRow(
        val uuid: String?,
        val categoryName: String,
        val title: String,
        val username: String,
        val password: String,
        val website: String,
        val notes: String
    )
}
