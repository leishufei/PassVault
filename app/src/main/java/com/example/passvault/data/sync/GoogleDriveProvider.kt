package com.example.passvault.data.sync

import android.content.Context
import android.content.Intent
import com.example.passvault.domain.sync.CloudStorageProvider
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
        private const val WEB_CLIENT_ID =
            "388522288499-mj21cvcfqkco2gngf3b17tle195qg3jq.apps.googleusercontent.com"
    }

    override val providerName: String = "Google Drive"

    private val gso: GoogleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestEmail()
        .requestScopes(Scope(DriveScopes.DRIVE_APPDATA))
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
            if (account != null && GoogleSignIn.hasPermissions(account, Scope(DriveScopes.DRIVE_APPDATA))) {
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

                // 查找是否已存在同名文件
                val existingFileId = findFileId(drive, remoteFileName)

                if (existingFileId != null) {
                    // 更新已有文件
                    val content = ByteArrayContent("application/json", fileData)
                    drive.files().update(existingFileId, null, content).execute()
                } else {
                    // 创建新文件
                    val fileMetadata = com.google.api.services.drive.model.File().apply {
                        name = remoteFileName
                        parents = listOf("appDataFolder")
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
                val fileId = findFileId(drive, remoteFileName)
                    ?: return@withContext Result.failure(Exception("Backup file not found on Drive"))

                val outputStream = java.io.ByteArrayOutputStream()
                drive.files().get(fileId).executeMediaAndDownloadTo(outputStream)
                Result.success(outputStream.toByteArray())
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    private fun findFileId(drive: Drive, fileName: String): String? {
        val result = drive.files().list()
            .setSpaces("appDataFolder")
            .setQ("name = '$fileName'")
            .setFields("files(id, name)")
            .setPageSize(1)
            .execute()
        return result.files?.firstOrNull()?.id
    }

    private fun buildDriveService(account: GoogleSignInAccount) {
        val credential = GoogleAccountCredential.usingOAuth2(
            context, listOf(DriveScopes.DRIVE_APPDATA)
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
