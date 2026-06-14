package com.example.passvault.domain.sync

import android.content.Intent

interface CloudStorageProvider {
    val providerName: String

    /** 构建登录 Intent，由 UI 层通过 ActivityResultLauncher 发起 */
    fun getSignInIntent(): Intent

    /** 处理登录结果，由 UI 层在 onActivityResult 回调中调用 */
    suspend fun handleSignInResult(data: Intent?): Result<Unit>

    /** 尝试静默恢复上一次的登录会话（App 重启后调用） */
    suspend fun trySilentSignIn(): Result<Unit>

    /** 当前是否已认证 */
    fun isAuthenticated(): Boolean

    /** 登出并清除凭据 */
    suspend fun signOut()

    suspend fun uploadBackup(fileData: ByteArray, remoteFileName: String): Result<Unit>
    suspend fun downloadBackup(remoteFileName: String): Result<ByteArray>

    /** 下载备份文件，同时返回文件版本标识（用于乐观锁） */
    suspend fun downloadBackupWithVersion(
        remoteFileName: String
    ): Result<Pair<ByteArray, String>>

    /** 带版本校验的上传。若云端文件版本与 expectedVersion 不匹配，返回失败 */
    suspend fun uploadBackupIfMatch(
        fileData: ByteArray,
        remoteFileName: String,
        expectedVersion: String
    ): Result<Unit>
}
