package com.example.passvault.autofill

import android.content.Context
import android.os.Build
import android.os.CancellationSignal
import android.service.autofill.AutofillService
import android.service.autofill.FillCallback
import android.service.autofill.FillRequest
import android.service.autofill.SaveCallback
import android.service.autofill.SaveRequest
import android.util.Log
import android.widget.inline.InlinePresentationSpec
import com.example.passvault.domain.auth.SessionManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.annotation.RequiresApi

@AndroidEntryPoint
@RequiresApi(Build.VERSION_CODES.O)
class PassVaultAutofillService : AutofillService() {

    @Inject lateinit var dataParser: AutofillDataParser
    @Inject lateinit var matchingEngine: AutofillMatchingEngine
    @Inject lateinit var responseBuilder: AutofillResponseBuilder
    @Inject lateinit var sessionManager: SessionManager

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }

    override fun onFillRequest(
        request: FillRequest,
        cancellationSignal: CancellationSignal,
        callback: FillCallback
    ) {
        val structure = request.fillContexts.lastOrNull()?.structure
        if (structure == null) {
            callback.onSuccess(null)
            return
        }

        val context = runCatching { dataParser.parse(structure) }
            .onFailure { Log.w(TAG, "Failed to parse autofill structure", it) }
            .getOrNull()
        if (context == null || !context.hasFillableFields) {
            callback.onSuccess(null)
            return
        }

        val inlineSpecs = collectInlineSpecs(request)

        val job = serviceScope.launch {
            val matches = runCatching { matchingEngine.findMatches(context) }
                .onFailure { Log.w(TAG, "Match lookup failed", it) }
                .getOrDefault(emptyList())

            if (matches.isEmpty()) {
                callback.onSuccess(null)
                return@launch
            }

            val response = if (sessionManager.isAutofillSessionValid) {
                responseBuilder.buildDatasetsResponse(context, matches, inlineSpecs)
            } else {
                responseBuilder.buildAuthenticationResponse(context, matches.size, inlineSpecs)
            }
            callback.onSuccess(response)
        }
        cancellationSignal.setOnCancelListener { job.cancel() }
    }

    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) {
        val structure = request.fillContexts.lastOrNull()?.structure
        if (structure == null) {
            callback.onSuccess()
            return
        }

        val ctx = runCatching { dataParser.parse(structure) }.getOrNull()
        if (ctx == null || !ctx.hasSavableValues) {
            callback.onSuccess()
            return
        }

        val intent = AutofillSaveActivity.newIntent(
            context = applicationContext,
            packageName = ctx.packageName,
            webDomain = ctx.webDomain,
            username = ctx.usernameValue.orEmpty(),
            password = ctx.passwordValue.orEmpty()
        ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        callback.onSuccess()
    }

    private fun collectInlineSpecs(request: FillRequest): List<InlinePresentationSpec>? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
        val prefs = applicationContext.getSharedPreferences(AUTOFILL_PREFS, Context.MODE_PRIVATE)
        val enabled = prefs.getBoolean(KEY_INLINE_ENABLED, true)
        if (!enabled) return null
        val inlineRequest = request.inlineSuggestionsRequest ?: return null
        return inlineRequest.inlinePresentationSpecs
    }

    companion object {
        private const val TAG = "PassVaultAutofill"
        private const val AUTOFILL_PREFS = "autofill_prefs"
        private const val KEY_INLINE_ENABLED = "inline_enabled"
    }
}
