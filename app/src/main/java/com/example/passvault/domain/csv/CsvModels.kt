package com.example.passvault.domain.csv

/**
 * Identifies which CSV format was supplied to the importer.
 */
enum class CsvFormat {
    /** New 7-column format with UUID column */
    EXTENDED,
    /** Legacy 4-column format used by older builds */
    LEGACY,
    /** Format could not be detected */
    UNKNOWN
}

/**
 * Indicates whether a row will create a new password or update an existing one.
 */
enum class ImportAction {
    INSERT,
    UPDATE
}

/**
 * Snapshot of an existing password's editable fields.
 * Used to compute field-level diffs against incoming CSV rows.
 */
data class ExistingPasswordSnapshot(
    val id: Long,
    val uuid: String,
    val title: String,
    val username: String,
    val password: String,
    val website: String,
    val notes: String,
    val categoryId: Long
)

/**
 * Field-level diff produced when updating an existing entry.
 */
data class FieldDiff(
    val field: String,
    val oldValue: String,
    val newValue: String
)

/**
 * A row that passed validation and is ready for import.
 */
data class ValidatedPasswordRow(
    val rowIndex: Int,
    val action: ImportAction,
    val title: String,
    val username: String,
    val password: String,
    val website: String,
    val notes: String,
    val categoryName: String,
    val existingId: Long? = null,
    val existingUuid: String? = null,
    val diffs: List<FieldDiff> = emptyList()
)

/**
 * A row that failed validation and will be skipped.
 */
data class InvalidRow(
    val rowIndex: Int,
    val rawData: List<String>,
    val errorMessage: String
)

/**
 * Outcome of validating a CSV file before performing the import.
 */
data class CsvValidationResult(
    val format: CsvFormat,
    val totalRows: Int,
    val validRows: List<ValidatedPasswordRow>,
    val invalidRows: List<InvalidRow>,
    val newCategoryNames: List<String>
) {
    val updateCount: Int get() = validRows.count { it.action == ImportAction.UPDATE && it.diffs.isNotEmpty() }
    val insertCount: Int get() = validRows.count { it.action == ImportAction.INSERT }
}

/**
 * Progress states emitted by the import pipeline so the UI can react.
 */
sealed class ImportProgress {
    object Idle : ImportProgress()
    object Validating : ImportProgress()
    data class ValidationComplete(val result: CsvValidationResult) : ImportProgress()
    data class Importing(val current: Int, val total: Int) : ImportProgress()
    data class Complete(val updated: Int, val inserted: Int) : ImportProgress()
    data class Error(val message: String) : ImportProgress()
}
