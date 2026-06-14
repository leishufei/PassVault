package com.example.passvault.data.sync

import android.content.Context
import android.content.Intent
import com.example.passvault.domain.sync.CloudStorageProvider
import com.example.passvault.BuildConfig
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.ByteArrayContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoogleDriveProvider @Inject constructor(
    @ApplicationContext private val context: Context
) : CloudStorageProvider {

    companion object {
        private val WEB_CLIENT_ID = BuildConfig.GOOGLE_WEB_CLIENT_ID
        private const val BACKUP_FOLDER_NAME = "PassVault"
    }

    override val providerName: String = "Google Drive"

    private val gso: GoogleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestEmail()
        .requestScopes(Scope(DriveScopes.DRIVE_FILE))
        .requestServerAuthCode(WEB_CLIENT_ID)
        .build()

    private val signInClient: GoogleSignInClient = GoogleSignIn.getClient(context, gso)

    private var driveService: Drive? = null

    override fun getSignInIntent(): Intent = signInClient.signInIntent

    override suspend fun handleSignInResult(data: Intent?): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
            buildDriveService(account)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun trySilentSignIn(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val account = GoogleSignIn.getLastSignedInAccount(context)
            if (account != null && GoogleSignIn.hasPermissions(account, Scope(DriveScopes.DRIVE_FILE))) {
                buildDriveService(account)
                Result.success(Unit)
            } else {
                Result.failure(Exception("No previous session"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun isAuthenticated(): Boolean = driveService != null

    override suspend fun signOut() {
        driveService = null
        signInClient.signOut()
    }

    override suspend fun uploadBackup(fileData: ByteArray, remoteFileName: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val drive = driveService ?: return@withContext Result.failure(Exception("Not authenticated"))

                // 获取或创建 PassVault 文件夹
                val folderId = getOrCreateFolder(drive)

                // 查找是否已存在同名文件
                val existingFileId = findFileId(drive, remoteFileName, folderId)

                if (existingFileId != null) {
                    // 更新已有文件
                    val content = ByteArrayContent("application/json", fileData)
                    drive.files().update(existingFileId, null, content).execute()
                } else {
                    // 创建新文件
                    val fileMetadata = com.google.api.services.drive.model.File().apply {
                        name = remoteFileName
                        parents = listOf(folderId)
                    }
                    val content = ByteArrayContent("application/json", fileData)
                    drive.files().create(fileMetadata, content)
                        .setFields("id")
                        .execute()
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun downloadBackup(remoteFileName: String): Result<ByteArray> =
        withContext(Dispatchers.IO) {
            try {
                val drive = driveService ?: return@withContext Result.failure(Exception("Not authenticated"))
                val folderId = getOrCreateFolder(drive)
                val fileId = findFileId(drive, remoteFileName, folderId)
                    ?: return@withContext Result.failure(Exception("Backup file not found on Drive"))

                val outputStream = java.io.ByteArrayOutputStream()
                drive.files().get(fileId).executeMediaAndDownloadTo(outputStream)
                Result.success(outputStream.toByteArray())
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    private fun findFileId(drive: Drive, fileName: String, folderId: String): String? {
        val result = drive.files().list()
            .setSpaces("drive")
            .setQ("name = '$fileName' and '$folderId' in parents and trashed = false")
            .setFields("files(id, name)")
            .setPageSize(1)
            .execute()
        return result.files?.firstOrNull()?.id
    }

    override suspend fun downloadBackupWithVersion(
        remoteFileName: String
    ): Result<Pair<ByteArray, String>> = withContext(Dispatchers.IO) {
        try {
            val drive = driveService ?: return@withContext Result.failure(Exception("Not authenticated"))
            val folderId = getOrCreateFolder(drive)
            val fileId = findFileId(drive, remoteFileName, folderId)
                ?: return@withContext Result.failure(Exception("Backup file not found on Drive"))

            // 获取文件元数据（含 modifiedTime 作为版本标识）
            val fileMeta = drive.files().get(fileId)
                .setFields("id, modifiedTime")
                .execute()
            val version = fileMeta.modifiedTime.toStringRfc3339()

            val outputStream = java.io.ByteArrayOutputStream()
            drive.files().get(fileId).executeMediaAndDownloadTo(outputStream)
            Result.success(Pair(outputStream.toByteArray(), version))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun uploadBackupIfMatch(
        fileData: ByteArray,
        remoteFileName: String,
        expectedVersion: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val drive = driveService ?: return@withContext Result.failure(Exception("Not authenticated"))
            val folderId = getOrCreateFolder(drive)
            val fileId = findFileId(drive, remoteFileName, folderId)

            if (fileId != null) {
                // 检查当前版本是否和下载时一致
                val currentMeta = drive.files().get(fileId)
                    .setFields("modifiedTime")
                    .execute()
                val currentVersion = currentMeta.modifiedTime.toStringRfc3339()

                if (currentVersion != expectedVersion) {
                    return@withContext Result.failure(
                        java.util.ConcurrentModificationException(
                            "云端文件已被其他设备修改 (expected=$expectedVersion, actual=$currentVersion)"
                        )
                    )
                }
                // 版本一致，安全更新
                val content = ByteArrayContent("application/json", fileData)
                drive.files().update(fileId, null, content).execute()
            } else {
                // 首次创建，无冲突风险
                val fileMetadata = com.google.api.services.drive.model.File().apply {
                    name = remoteFileName
                    parents = listOf(folderId)
                }
                val content = ByteArrayContent("application/json", fileData)
                drive.files().create(fileMetadata, content)
                    .setFields("id")
                    .execute()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun getOrCreateFolder(drive: Drive): String {
        // 查找已有的 PassVault 文件夹
        val result = drive.files().list()
            .setSpaces("drive")
            .setQ("name = '$BACKUP_FOLDER_NAME' and mimeType = 'application/vnd.google-apps.folder' and trashed = false")
            .setFields("files(id, name)")
            .setPageSize(1)
            .execute()
        val existingFolder = result.files?.firstOrNull()
        if (existingFolder != null) return existingFolder.id

        // 创建新的 PassVault 文件夹
        val folderMetadata = com.google.api.services.drive.model.File().apply {
            name = BACKUP_FOLDER_NAME
            mimeType = "application/vnd.google-apps.folder"
        }
        val folder = drive.files().create(folderMetadata)
            .setFields("id")
            .execute()
        return folder.id
    }

    private fun buildDriveService(account: GoogleSignInAccount) {
        val credential = GoogleAccountCredential.usingOAuth2(
            context, listOf(DriveScopes.DRIVE_FILE)
        )
        credential.selectedAccount = account.account
        driveService = Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        )
            .setApplicationName("PassVault")
            .build()
    }
}
