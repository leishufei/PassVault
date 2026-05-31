package com.example.passvault.domain.sync

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
    private val cloudCryptoManager: CloudCryptoManager,
    private val sessionManager: SessionManager
) {
    private val gson = Gson()
    
    var currentProvider: CloudStorageProvider? = null

    data class SyncPayload(
        val passwords: List<PasswordSyncItem>
    )

    data class PasswordSyncItem(
        val title: String,
        val username: String,
        val password: String,
        val website: String,
        val notes: String,
        val categoryId: Long,
        val isFavorite: Boolean,
        val iconColor: Int,
        val strength: Int,
        val createdAt: Long,
        val updatedAt: Long,
        val isDeleted: Boolean
    )

    suspend fun performSync(remoteFileName: String = "PassVault_Cloud_Backup.json"): Result<Unit> {
        val provider = currentProvider ?: return Result.failure(Exception("No cloud provider selected"))
        val masterPassword = sessionManager.currentMasterPassword ?: return Result.failure(Exception("Session is locked"))

        return try {
            // 1. Get Local Items
            val localEntries = passwordRepository.getAllPasswordsForSync().first()
            val localItems = localEntries.map { 
                PasswordSyncItem(
                    title = it.title,
                    username = it.username,
                    password = it.password,
                    website = it.website,
                    notes = it.notes,
                    categoryId = it.categoryId,
                    isFavorite = it.isFavorite,
                    iconColor = it.iconColor,
                    strength = it.strength,
                    createdAt = it.createdAt,
                    updatedAt = it.updatedAt,
                    isDeleted = it.isDeleted
                )
            }
            val localMap = localItems.associateBy { "${it.categoryId}::${it.title}::${it.username}" }

            // 2. Download Cloud Items
            val cloudItems = try {
                val fileData = provider.downloadBackup(remoteFileName).getOrThrow()
                val backupJson = String(fileData, Charsets.UTF_8)
                val backupFormat = gson.fromJson(backupJson, CloudBackupFormat::class.java)
                val payloadJson = cloudCryptoManager.decryptFromCloud(backupFormat, masterPassword)
                gson.fromJson(payloadJson, SyncPayload::class.java).passwords
            } catch (e: Exception) {
                emptyList<PasswordSyncItem>()
            }
            val cloudMap = cloudItems.associateBy { "${it.categoryId}::${it.title}::${it.username}" }

            // 3. Two-Way Merge
            val mergedMap = mutableMapOf<String, PasswordSyncItem>()

            cloudItems.forEach { cloudItem ->
                val key = "${cloudItem.categoryId}::${cloudItem.title}::${cloudItem.username}"
                val localItem = localMap[key]
                if (localItem == null) {
                    mergedMap[key] = cloudItem // New from cloud
                } else {
                    // Conflict resolution by updatedAt
                    if (cloudItem.updatedAt > localItem.updatedAt) {
                        mergedMap[key] = cloudItem
                    } else {
                        mergedMap[key] = localItem
                    }
                }
            }

            localItems.forEach { localItem ->
                val key = "${localItem.categoryId}::${localItem.title}::${localItem.username}"
                if (!cloudMap.containsKey(key)) {
                    mergedMap[key] = localItem // New from local
                }
            }

            val mergedItems = mergedMap.values.toList()

            // 4. Update Local Database
            passwordRepository.syncMergedPasswords(mergedItems)

            // 5. Encrypt and Upload Merged Data
            val payloadJson = gson.toJson(SyncPayload(mergedItems))
            val backupFormat = cloudCryptoManager.encryptForCloud(payloadJson, masterPassword)
            val backupJson = gson.toJson(backupFormat)
            provider.uploadBackup(backupJson.toByteArray(Charsets.UTF_8), remoteFileName)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
