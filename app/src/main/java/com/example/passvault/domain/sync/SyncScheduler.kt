package com.example.passvault.domain.sync

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 同步调度器：通过防抖机制避免短时间内多次修改触发多次全量同步。
 *
 * - markDirty()：标记有本地变更，延迟 DEBOUNCE_MS 后触发同步
 * - syncImmediately()：立即同步（手动触发 / 登录后）
 */
@Singleton
class SyncScheduler @Inject constructor(
    private val syncManager: SyncManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var debounceJob: Job? = null

    companion object {
        private const val DEBOUNCE_MS = 5000L // 5秒防抖
        private const val TAG = "SyncScheduler"
    }

    /** 标记有本地变更，延迟触发同步 */
    fun markDirty() {
        if (syncManager.currentProvider == null) return
        debounceJob?.cancel()
        debounceJob = scope.launch {
            Log.d(TAG, "变更已标记，${DEBOUNCE_MS}ms 后触发同步")
            delay(DEBOUNCE_MS)
            Log.d(TAG, "防抖结束，开始同步")
            syncManager.performSync()
        }
    }

    /** 立即同步（取消待执行的防抖任务） */
    suspend fun syncImmediately(): Result<Unit> {
        debounceJob?.cancel()
        return syncManager.performSync()
    }
}
