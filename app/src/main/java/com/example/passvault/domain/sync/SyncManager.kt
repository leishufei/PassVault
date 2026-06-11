package com.example.passvault.domain.sync

import com.example.passvault.BuildConfig
import com.example.passvault.data.repository.CategoryRepository
import com.example.passvault.data.repository.PasswordImportData
import com.example.passvault.data.repository.PasswordRepository
import com.example.passvault.data.security.CloudBackupFormat
import com.example.passvault.data.security.CloudCryptoManager
import com.example.passvault.domain.auth.SessionManager
import com.google.gson.Gson
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncManager @Inject constructor(
    private val passwordRepository: PasswordRepository,
    private val categoryRepository: CategoryRepository,
    private val cloudCryptoManager: CloudCryptoManager,
    private val sessionManager: SessionManager
) {
    private val gson = Gson()
    
    var currentProvider: CloudStorageProvider? = null

    data class CategorySyncItem(
        val name: String,
        val color: Int,
        val sortOrder: Int
    )

    data class SyncPayload(
        val passwords: List<PasswordSyncItem>,
        val categories: List<CategorySyncItem>? = null
    )

    data class PasswordSyncItem(
        val title: String,
        val username: String,
        val password: String,
        val website: String,
        val notes: String,
        val categoryName: String,
        val isFavorite: Boolean,
        val iconColor: Int,
        val createdAt: Long,
        val updatedAt: Long,
        val isDeleted: Boolean
    )

    suspend fun performSync(remoteFileName: String = "PassVault_Cloud_Backup.json"): Result<Unit> {
        val provider = currentProvider ?: return Result.failure(Exception("No cloud provider selected"))
        val masterPassword = sessionManager.currentMasterPassword
            ?: BuildConfig.DEV_MASTER_PASSWORD.ifEmpty { null }
            ?: return Result.failure(Exception("Session is locked"))

        return try {
            val localCategories = categoryRepository.getAllCategories().first()
            val categoryIdToName = localCategories.associate { it.id to it.name }

            // 1. Build local password items
            val localEntries = passwordRepository.getAllPasswordsForSync().first()
            val localItems = localEntries.map {
                PasswordSyncItem(
                    title = it.title,
                    username = it.username,
                    password = it.password,
                    website = it.website,
                    notes = it.notes,
                    categoryName = categoryIdToName[it.categoryId] ?: "未分类",
                    isFavorite = it.isFavorite,
                    iconColor = it.iconColor,
                    createdAt = it.createdAt,
                    updatedAt = it.updatedAt,
                    isDeleted = it.isDeleted
                )
            }
            val localMap = localItems.associateBy { "${it.categoryName}::${it.title}::${it.username}" }

            // 2. Download cloud payload
            val cloudPayload = try {
                val fileData = provider.downloadBackup(remoteFileName).getOrThrow()
                val backupJson = String(fileData, Charsets.UTF_8)
                val backupFormat = gson.fromJson(backupJson, CloudBackupFormat::class.java)
                val payloadJson = cloudCryptoManager.decryptFromCloud(backupFormat, masterPassword)
                gson.fromJson(payloadJson, SyncPayload::class.java)
            } catch (e: Exception) {
                null
            }
            val cloudItems = cloudPayload?.passwords ?: emptyList()
            val cloudCategories = cloudPayload?.categories ?: emptyList()

            // 3. Merge categories — create any that are missing locally
            val localCategoryNames = localCategories.map { it.name }.toMutableSet()
            val categoryNamesToCreate = mutableListOf<CategorySyncItem>()

            cloudCategories.forEach { cloudCat ->
                if (cloudCat.name !in localCategoryNames && cloudCat.name != "未分类") {
                    categoryNamesToCreate.add(cloudCat)
                    localCategoryNames.add(cloudCat.name)
                }
            }

            // Fallback for old-format files without categories field
            if (cloudCategories.isEmpty()) {
                cloudItems.map { it.categoryName }.distinct().forEach { name ->
                    if (name != "未分类" && name !in localCategoryNames) {
                        categoryNamesToCreate.add(CategorySyncItem(name, (0..5).random(), 0))
                        localCategoryNames.add(name)
                    }
                }
            }

            categoryNamesToCreate.forEach { cat ->
                categoryRepository.insertCategory(cat.name, cat.color, cat.sortOrder)
            }

            // Rebuild mapping after inserts
            val updatedCategories = categoryRepository.getAllCategories().first()
            val categoryNameToId = updatedCategories.associate { it.name to it.id }

            // 4. Two-way merge passwords
            val cloudMap = cloudItems.associateBy { "${it.categoryName}::${it.title}::${it.username}" }
            val mergedMap = mutableMapOf<String, PasswordSyncItem>()

            cloudItems.forEach { cloudItem ->
                val key = "${cloudItem.categoryName}::${cloudItem.title}::${cloudItem.username}"
                val localItem = localMap[key]
                if (localItem == null) {
                    mergedMap[key] = cloudItem
                } else {
                    mergedMap[key] = if (cloudItem.updatedAt > localItem.updatedAt) cloudItem else localItem
                }
            }

            localItems.forEach { localItem ->
                val key = "${localItem.categoryName}::${localItem.title}::${localItem.username}"
                if (!cloudMap.containsKey(key)) {
                    mergedMap[key] = localItem
                }
            }

            val mergedItems = mergedMap.values.toList()

            // 5. Update local database
            passwordRepository.syncMergedPasswords(mergedItems, categoryNameToId)

            // 6. Build merged category list for upload
            val mergedCategoryItems = updatedCategories.map {
                CategorySyncItem(name = it.name, color = it.color, sortOrder = it.sortOrder)
            }

            // 7. Encrypt and upload
            val payloadJson = gson.toJson(SyncPayload(mergedItems, mergedCategoryItems))
            val backupFormat = cloudCryptoManager.encryptForCloud(payloadJson, masterPassword)
            val backupJson = gson.toJson(backupFormat)
            provider.uploadBackup(backupJson.toByteArray(Charsets.UTF_8), remoteFileName)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
