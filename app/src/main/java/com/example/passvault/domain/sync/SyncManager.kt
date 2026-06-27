package com.example.passvault.domain.sync

import android.util.Log
import com.example.passvault.BuildConfig
import com.example.passvault.data.local.CategoryDao
import com.example.passvault.data.local.PasswordDao
import com.example.passvault.data.model.Category
import com.example.passvault.data.model.PasswordEntry
import com.example.passvault.data.repository.CategoryRepository
import com.example.passvault.data.repository.PasswordRepository
import com.example.passvault.data.security.CloudBackupFormat
import com.example.passvault.data.security.CloudCryptoManager
import com.example.passvault.data.security.CryptoManager
import com.example.passvault.data.security.EncryptedData
import com.example.passvault.domain.auth.SessionManager
import com.google.gson.Gson
import kotlinx.coroutines.flow.first
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncManager @Inject constructor(
    private val passwordDao: PasswordDao,
    private val categoryDao: CategoryDao,
    private val categoryRepository: CategoryRepository,
    private val cryptoManager: CryptoManager,
    private val cloudCryptoManager: CloudCryptoManager,
    private val sessionManager: SessionManager
) {
    private val gson = Gson()

    var currentProvider: CloudStorageProvider? = null

    // ── V2 同步数据结构 ──

    data class SyncPayloadV2(
        val version: Int = 2,
        val syncTimestamp: Long = System.currentTimeMillis(),
        val passwords: List<PasswordSyncItemV2>,
        val categories: List<CategorySyncItemV2>
    )

    data class PasswordSyncItemV2(
        val uuid: String,
        val title: String,
        val username: String,
        val password: String,
        val website: String,
        val notes: String,
        val categoryUuid: String,
        val isFavorite: Boolean,
        val iconColor: Int,
        val createdAt: Long,
        val updatedAt: Long,
        val isDeleted: Boolean
    )

    data class CategorySyncItemV2(
        val uuid: String,
        val name: String,
        val color: Int,
        val sortOrder: Int,
        val createdAt: Long,
        val updatedAt: Long,
        val isDeleted: Boolean
    )

    // ── V1 兼容数据结构（只用于读取旧格式） ──

    private data class SyncPayloadV1(
        val passwords: List<PasswordSyncItemV1>?,
        val categories: List<CategorySyncItemV1>?
    )

    private data class PasswordSyncItemV1(
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

    private data class CategorySyncItemV1(
        val name: String,
        val color: Int,
        val sortOrder: Int
    )

    // ── 核心同步流程 ──

    /** 带冲突重试的同步入口 */
    suspend fun performSync(
        remoteFileName: String = "PassVault_Cloud_Backup.json",
        maxRetries: Int = 3
    ): Result<Unit> {
        repeat(maxRetries) { attempt ->
            val result = doSyncOnce(remoteFileName)
            if (result.isSuccess) return result
            val error = result.exceptionOrNull()
            if (error is java.util.ConcurrentModificationException) {
                Log.w("SyncV2", "并发冲突，重试 ${attempt + 1}/$maxRetries")
                // 继续下一轮重试（重新下载合并后再上传）
            } else {
                return result // 非冲突错误，直接返回
            }
        }
        return Result.failure(Exception("同步失败：多次重试后仍存在并发冲突"))
    }

    /** 云端文件版本标识，用于乐观锁 */
    private var lastCloudVersion: String? = null

    private suspend fun doSyncOnce(remoteFileName: String): Result<Unit> {
        val provider = currentProvider ?: return Result.failure(Exception("No cloud provider selected"))
        val masterPassword = sessionManager.currentMasterPassword
            ?: BuildConfig.DEV_MASTER_PASSWORD.ifEmpty { null }
            ?: return Result.failure(Exception("Session is locked"))

        return try {
            // 1. 构建本地数据快照（含已删除记录）
            val localPasswords = passwordDao.getAllPasswordsIncludingDeleted()
            val localCategories = categoryDao.getAllCategoriesIncludingDeleted()
            val categoryIdToUuid = localCategories.associate { it.id to it.uuid }.toMutableMap()
            categoryIdToUuid[0L] = "" // 未分类的特殊映射
            Log.d("SyncV2", "本地: ${localPasswords.size} 条密码, ${localCategories.size} 个分类")

            // 2. 下载云端数据（带版本标识）
            val downloadResult = downloadAndDecrypt(provider, remoteFileName, masterPassword)
            val cloudPayload = downloadResult?.first
            lastCloudVersion = downloadResult?.second

            // 3. 先合并分类
            val localCatMap = localCategories.associate { it.uuid to it }
            val (mergedCategories, catChanged) = if (cloudPayload != null) {
                val cloudCatMap = cloudPayload.categories.associate { it.uuid to it }
                mergeCategories(localCatMap, cloudCatMap)
            } else {
                Pair(localCategories.map { it.toCategorySyncItem() }, false)
            }

            // 写入本地 DB 并重建映射
            applyCategoryMerge(mergedCategories)
            val updatedCategories = categoryDao.getAllCategoriesIncludingDeleted()
            val newCategoryUuidToId = updatedCategories.associate { it.uuid to it.id }.toMutableMap()
            newCategoryUuidToId[""] = 0L // 未分类的特殊映射

            // 4. 合并密码
            val localPwItems = localPasswords.map { it.toPasswordSyncItem(categoryIdToUuid) }
            val localPwMap = localPwItems.associate { it.uuid to it }
            val (mergedPasswords, pwChanged) = if (cloudPayload != null) {
                val cloudPwMap = cloudPayload.passwords.associate { it.uuid to it }
                mergeItems(localPwMap, cloudPwMap)
            } else {
                Pair(localPwItems, false)
            }

            // 写入本地 DB
            applyPasswordMerge(mergedPasswords, newCategoryUuidToId)

            // 4.5. 同名分类去重：将 loser 下的密码迁移至 winner，逻辑删除 loser
            categoryRepository.dedupByName()
            val dedupedCategories = categoryDao.getAllCategoriesIncludingDeleted()
            val dedupedPasswords = passwordDao.getAllPasswordsIncludingDeleted()
            val dedupCatChanged = dedupedCategories.size != updatedCategories.size ||
                dedupedCategories.any { c -> c.isDeleted && updatedCategories.firstOrNull { it.uuid == c.uuid }?.isDeleted == false }
            val dedupedCatIdToUuid = dedupedCategories.associate { it.id to it.uuid }.toMutableMap()
            dedupedCatIdToUuid[0L] = ""
            val finalCategories = dedupedCategories.map { it.toCategorySyncItem() }
            val finalPasswords = dedupedPasswords.map { it.toPasswordSyncItem(dedupedCatIdToUuid) }

            Log.d("SyncV2", "合并结果: ${finalPasswords.size} 条密码, ${finalCategories.size} 个分类")

            // 5. 上传（乐观锁）
            val needUpload = catChanged || pwChanged || dedupCatChanged || cloudPayload == null
            if (needUpload) {
                val payload = SyncPayloadV2(
                    passwords = finalPasswords,
                    categories = finalCategories
                )
                encryptAndUpload(provider, remoteFileName, masterPassword, payload)
                Log.d("SyncV2", "已上传合并后的数据")
            } else {
                Log.d("SyncV2", "云端已是最新，跳过上传")
            }

            // 6. 清理过期墓碑（30天）
            cleanupOldTombstones()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("SyncV2", "同步失败", e)
            Result.failure(e)
        }
    }

    // ── 下载与解密 ──

    /** 下载并解密，返回 Pair<Payload, VersionTag>；VersionTag 用于乐观锁 */
    private suspend fun downloadAndDecrypt(
        provider: CloudStorageProvider,
        remoteFileName: String,
        masterPassword: String
    ): Pair<SyncPayloadV2, String>? {
        // 优先使用带版本的 API
        val (fileData, version) = try {
            provider.downloadBackupWithVersion(remoteFileName).getOrThrow()
        } catch (e: Exception) {
            // 降级：使用无版本的 API（云端无文件时）
            Log.d("SyncV2", "云端无文件或下载失败: ${e.message}")
            return null
        }

        val backupJson = String(fileData, Charsets.UTF_8)
        val backupFormat = gson.fromJson(backupJson, CloudBackupFormat::class.java)
        val payloadJson = cloudCryptoManager.decryptFromCloud(backupFormat, masterPassword)

        // 尝试解析为 V2
        val payload = try {
            val v2 = gson.fromJson(payloadJson, SyncPayloadV2::class.java)
            if (v2.version == 2 && v2.passwords != null) {
                v2
            } else {
                migrateV1ToV2(payloadJson)
            }
        } catch (e: Exception) {
            migrateV1ToV2(payloadJson)
        }

        return payload?.let { Pair(it, version) }
    }

    /** 将 V1 格式（无 uuid）转换为 V2 格式 */
    private suspend fun migrateV1ToV2(payloadJson: String): SyncPayloadV2? {
        return try {
            val v1 = gson.fromJson(payloadJson, SyncPayloadV1::class.java) ?: return null
            val v1Passwords = v1.passwords ?: emptyList()
            val v1Categories = v1.categories ?: emptyList()
            Log.d("SyncV2", "检测到 V1 格式，开始迁移: ${v1Passwords.size} 条密码")

            // 读取本地数据用于 UUID 匹配
            val localPasswords = passwordDao.getAllPasswordsIncludingDeleted()
            val localCategories = categoryDao.getAllCategoriesIncludingDeleted()

            // 分类迁移：按 name 匹配本地 UUID，匹配不到则生成新 UUID
            val catNameToUuid = localCategories.associate { it.name to it.uuid }
            val migratedCategories = v1Categories.map { v1Cat ->
                CategorySyncItemV2(
                    uuid = catNameToUuid[v1Cat.name] ?: UUID.randomUUID().toString(),
                    name = v1Cat.name,
                    color = v1Cat.color,
                    sortOrder = v1Cat.sortOrder,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                    isDeleted = false
                )
            }
            val migratedCatNameToUuid = migratedCategories.associate { it.name to it.uuid }

            // 密码迁移：按 title+username 匹配本地 UUID
            val localPwLookup = localPasswords.associate { "${it.title}::${it.username}" to it }
            val migratedPasswords = v1Passwords.map { v1Pw ->
                val localMatch = localPwLookup["${v1Pw.title}::${v1Pw.username}"]
                PasswordSyncItemV2(
                    uuid = localMatch?.uuid ?: UUID.randomUUID().toString(),
                    title = v1Pw.title,
                    username = v1Pw.username,
                    password = v1Pw.password,
                    website = v1Pw.website,
                    notes = v1Pw.notes,
                    categoryUuid = migratedCatNameToUuid[v1Pw.categoryName]
                        ?: catNameToUuid[v1Pw.categoryName]
                        ?: "",
                    isFavorite = v1Pw.isFavorite,
                    iconColor = v1Pw.iconColor,
                    createdAt = v1Pw.createdAt,
                    updatedAt = v1Pw.updatedAt,
                    isDeleted = v1Pw.isDeleted
                )
            }

            SyncPayloadV2(
                version = 2,
                passwords = migratedPasswords,
                categories = migratedCategories
            )
        } catch (e: Exception) {
            Log.e("SyncV2", "V1 迁移失败", e)
            null
        }
    }

    // ── 合并算法 ──

    /** 通用合并：基于 UUID + updatedAt 裁决 */
    private fun <T> mergeByUuid(
        localMap: Map<String, T>,
        cloudMap: Map<String, T>,
        getUpdatedAt: (T) -> Long
    ): Pair<List<T>, Boolean> {
        val allUuids = localMap.keys + cloudMap.keys
        val merged = mutableListOf<T>()
        var hasLocalChanges = false

        allUuids.forEach { uuid ->
            val local = localMap[uuid]
            val cloud = cloudMap[uuid]
            when {
                cloud == null && local != null -> {
                    merged.add(local)
                    hasLocalChanges = true // 本地独有，需要上传
                }
                local == null && cloud != null -> {
                    merged.add(cloud) // 云端独有，需要写入本地
                }
                local != null && cloud != null -> {
                    if (getUpdatedAt(local) >= getUpdatedAt(cloud)) {
                        merged.add(local)
                        if (getUpdatedAt(local) > getUpdatedAt(cloud)) {
                            hasLocalChanges = true
                        }
                    } else {
                        merged.add(cloud)
                    }
                }
            }
        }
        return Pair(merged, hasLocalChanges)
    }

    private fun mergeItems(
        localMap: Map<String, PasswordSyncItemV2>,
        cloudMap: Map<String, PasswordSyncItemV2>
    ): Pair<List<PasswordSyncItemV2>, Boolean> {
        return mergeByUuid(localMap, cloudMap) { it.updatedAt }
    }

    private fun mergeCategories(
        localMap: Map<String, Category>,
        cloudMap: Map<String, CategorySyncItemV2>
    ): Pair<List<CategorySyncItemV2>, Boolean> {
        val localSyncMap = localMap.mapValues { (_, cat) -> cat.toCategorySyncItem() }
        return mergeByUuid(localSyncMap, cloudMap) { it.updatedAt }
    }

    // ── 写入本地 DB ──

    private suspend fun applyCategoryMerge(mergedCategories: List<CategorySyncItemV2>) {
        mergedCategories.forEach { syncCat ->
            val existing = categoryDao.getCategoryByUuid(syncCat.uuid)
            val category = Category(
                id = existing?.id ?: 0L,
                uuid = syncCat.uuid,
                name = syncCat.name,
                color = syncCat.color,
                isDefault = existing?.isDefault ?: false,
                sortOrder = syncCat.sortOrder,
                createdAt = syncCat.createdAt,
                updatedAt = syncCat.updatedAt,
                isDeleted = syncCat.isDeleted
            )
            categoryDao.insertCategory(category) // REPLACE 策略
        }
    }

    private suspend fun applyPasswordMerge(
        mergedPasswords: List<PasswordSyncItemV2>,
        categoryUuidToId: Map<String, Long>
    ) {
        mergedPasswords.forEach { syncPw ->
            val categoryId = categoryUuidToId[syncPw.categoryUuid] ?: 0L
            val existing = passwordDao.getPasswordByUuid(syncPw.uuid)

            if (existing == null && !syncPw.isDeleted &&
                (syncPw.title.isNotEmpty() || syncPw.username.isNotEmpty()) &&
                passwordDao.countDuplicate(syncPw.title, syncPw.username, 0L) > 0) {
                Log.d("SyncV2", "Skip duplicate insert: ${syncPw.title}/${syncPw.username}")
                return@forEach
            }

            val encryptedData = cryptoManager.encrypt(syncPw.password)

            val entry = PasswordEntry(
                id = existing?.id ?: 0L,
                uuid = syncPw.uuid,
                title = syncPw.title,
                username = syncPw.username,
                encryptedPassword = encryptedData.ciphertext,
                passwordIv = encryptedData.iv,
                website = syncPw.website,
                notes = syncPw.notes,
                isFavorite = syncPw.isFavorite,
                iconColor = syncPw.iconColor,
                strength = calculatePasswordStrength(syncPw.password),
                categoryId = categoryId,
                createdAt = syncPw.createdAt,
                updatedAt = syncPw.updatedAt,
                isDeleted = syncPw.isDeleted
            )
            passwordDao.insertPassword(entry)
        }
    }

    // ── 加密上传 ──

    private suspend fun encryptAndUpload(
        provider: CloudStorageProvider,
        remoteFileName: String,
        masterPassword: String,
        payload: SyncPayloadV2
    ) {
        val payloadJson = gson.toJson(payload)
        val backupFormat = cloudCryptoManager.encryptForCloud(payloadJson, masterPassword)
        val backupJson = gson.toJson(backupFormat)
        val fileBytes = backupJson.toByteArray(Charsets.UTF_8)

        val version = lastCloudVersion
        if (version != null) {
            // 乐观锁上传：版本不匹配时抛出 ConcurrentModificationException
            provider.uploadBackupIfMatch(fileBytes, remoteFileName, version).getOrThrow()
        } else {
            // 首次上传（云端无文件），直接上传
            provider.uploadBackup(fileBytes, remoteFileName).getOrThrow()
        }
    }

    // ── 墓碑清理 ──

    private suspend fun cleanupOldTombstones() {
        val cutoff = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
        passwordDao.deleteOldTombstones(cutoff)
        categoryDao.deleteOldTombstones(cutoff)
    }

    // ── 工具方法 ──

    private fun PasswordEntry.toPasswordSyncItem(
        categoryIdToUuid: Map<Long, String>
    ): PasswordSyncItemV2 {
        val decryptedPassword = cryptoManager.decrypt(
            EncryptedData(encryptedPassword, passwordIv)
        )
        return PasswordSyncItemV2(
            uuid = uuid,
            title = title,
            username = username,
            password = decryptedPassword,
            website = website,
            notes = notes,
            categoryUuid = categoryIdToUuid[categoryId] ?: "",
            isFavorite = isFavorite,
            iconColor = iconColor,
            createdAt = createdAt,
            updatedAt = updatedAt,
            isDeleted = isDeleted
        )
    }

    private fun Category.toCategorySyncItem(): CategorySyncItemV2 {
        return CategorySyncItemV2(
            uuid = uuid,
            name = name,
            color = color,
            sortOrder = sortOrder,
            createdAt = createdAt,
            updatedAt = updatedAt,
            isDeleted = isDeleted
        )
    }

    private fun calculatePasswordStrength(password: String): Int {
        val length = password.length
        val hasUppercase = password.any { it.isUpperCase() }
        val hasLowercase = password.any { it.isLowerCase() }
        val hasDigit = password.any { it.isDigit() }
        val hasSpecial = password.any { !it.isLetterOrDigit() }
        val charTypeCount = listOf(hasUppercase, hasLowercase, hasDigit, hasSpecial).count { it }
        return when {
            length >= 12 && charTypeCount >= 3 -> 3
            length >= 8 && charTypeCount >= 2 -> 2
            else -> 1
        }
    }
}
