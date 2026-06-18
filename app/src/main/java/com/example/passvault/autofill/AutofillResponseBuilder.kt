package com.example.passvault.autofill

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.autofill.Dataset
import android.service.autofill.FillResponse
import android.service.autofill.InlinePresentation
import android.service.autofill.SaveInfo
import android.view.autofill.AutofillValue
import android.widget.RemoteViews
import android.widget.inline.InlinePresentationSpec
import androidx.annotation.RequiresApi
import androidx.autofill.inline.v1.InlineSuggestionUi
import com.example.passvault.R
import com.example.passvault.data.model.PasswordEntry
import com.example.passvault.data.repository.PasswordRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@RequiresApi(Build.VERSION_CODES.O)
class AutofillResponseBuilder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: PasswordRepository
) {

    fun buildAuthenticationResponse(
        autofillContext: AutofillContext,
        matchCount: Int,
        inlineSpecs: List<InlinePresentationSpec>?
    ): FillResponse? {
        val autofillIds = listOfNotNull(
            autofillContext.usernameFieldId,
            autofillContext.passwordFieldId
        ).toTypedArray()
        if (autofillIds.isEmpty()) return null

        val authIntent = AutofillAuthActivity.newIntent(
            context = context,
            packageName = autofillContext.packageName,
            webDomain = autofillContext.webDomain,
            usernameFieldId = autofillContext.usernameFieldId,
            passwordFieldId = autofillContext.passwordFieldId
        )
        val authPendingIntent = pendingActivityIntent(REQ_AUTH, authIntent)

        val unlockPresentation = remoteUnlockView(
            text = context.getString(R.string.autofill_unlock_title, matchCount)
        )

        val builder = FillResponse.Builder()
        val inline = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !inlineSpecs.isNullOrEmpty()) {
            inlineUnlockPresentation(inlineSpecs.first(), matchCount, authPendingIntent)
        } else null

        if (inline != null) {
            builder.setAuthentication(autofillIds, authPendingIntent.intentSender, unlockPresentation, inline)
        } else {
            builder.setAuthentication(autofillIds, authPendingIntent.intentSender, unlockPresentation)
        }

        addSaveInfo(builder, autofillContext)
        return builder.build()
    }

    fun buildDatasetsResponse(
        autofillContext: AutofillContext,
        matches: List<PasswordEntry>,
        inlineSpecs: List<InlinePresentationSpec>?
    ): FillResponse? {
        if (matches.isEmpty()) return null

        val builder = FillResponse.Builder()
        matches.forEachIndexed { index, entry ->
            val plaintext = runCatching { repository.decryptPassword(entry) }.getOrNull() ?: return@forEachIndexed
            val dataset = buildDataset(autofillContext, entry, plaintext, index, inlineSpecs)
            if (dataset != null) builder.addDataset(dataset)
        }

        addSaveInfo(builder, autofillContext)
        return runCatching { builder.build() }.getOrNull()
    }

    private fun buildDataset(
        autofillContext: AutofillContext,
        entry: PasswordEntry,
        plaintext: String,
        index: Int,
        inlineSpecs: List<InlinePresentationSpec>?
    ): Dataset? {
        val datasetBuilder = Dataset.Builder()
        datasetBuilder.setId(entry.id.toString())
        val itemPresentation = remoteListItem(
            title = entry.title.ifBlank { context.getString(R.string.app_name) },
            subtitle = entry.username
        )

        val inline = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !inlineSpecs.isNullOrEmpty()) {
            val spec = inlineSpecs.getOrNull(index) ?: inlineSpecs.last()
            inlineDatasetPresentation(spec, entry)
        } else null

        autofillContext.usernameFieldId?.let { id ->
            val value = AutofillValue.forText(entry.username)
            if (inline != null) {
                datasetBuilder.setValue(id, value, itemPresentation, inline)
            } else {
                datasetBuilder.setValue(id, value, itemPresentation)
            }
        }
        autofillContext.passwordFieldId?.let { id ->
            val value = AutofillValue.forText(plaintext)
            if (inline != null) {
                datasetBuilder.setValue(id, value, itemPresentation, inline)
            } else {
                datasetBuilder.setValue(id, value, itemPresentation)
            }
        }

        return runCatching { datasetBuilder.build() }.getOrNull()
    }

    private fun addSaveInfo(builder: FillResponse.Builder, ctx: AutofillContext) {
        val saveTargets = listOfNotNull(ctx.usernameFieldId, ctx.passwordFieldId).toTypedArray()
        if (saveTargets.isEmpty() || ctx.passwordFieldId == null) return
        val type = SaveInfo.SAVE_DATA_TYPE_USERNAME or SaveInfo.SAVE_DATA_TYPE_PASSWORD
        val saveInfo = SaveInfo.Builder(type, saveTargets).build()
        builder.setSaveInfo(saveInfo)
    }

    private fun remoteListItem(title: String, subtitle: String): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.autofill_list_item)
        views.setTextViewText(R.id.autofill_item_title, title)
        views.setTextViewText(R.id.autofill_item_subtitle, subtitle)
        return views
    }

    private fun remoteUnlockView(text: String): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.autofill_unlock_item)
        views.setTextViewText(R.id.autofill_item_title, text)
        return views
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun inlineDatasetPresentation(spec: InlinePresentationSpec, entry: PasswordEntry): InlinePresentation? {
        return runCatching {
            val pi = attributionPendingIntent()
            val slice = InlineSuggestionUi.newContentBuilder(pi)
                .setTitle(entry.title.ifBlank { context.getString(R.string.app_name) })
                .setSubtitle(entry.username)
                .build()
                .slice
            InlinePresentation(slice, spec, false)
        }.getOrNull()
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun inlineUnlockPresentation(
        spec: InlinePresentationSpec,
        matchCount: Int,
        authPendingIntent: PendingIntent
    ): InlinePresentation? {
        return runCatching {
            val slice = InlineSuggestionUi.newContentBuilder(authPendingIntent)
                .setTitle(context.getString(R.string.app_name))
                .setSubtitle(context.getString(R.string.autofill_unlock_inline_subtitle, matchCount))
                .build()
                .slice
            InlinePresentation(slice, spec, false)
        }.getOrNull()
    }

    private fun pendingActivityIntent(requestCode: Int, intent: Intent): PendingIntent {
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        return PendingIntent.getActivity(context, requestCode, intent, flags)
    }

    private fun attributionPendingIntent(): PendingIntent {
        val intent = Intent(context, com.example.passvault.MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getActivity(context, REQ_INLINE_ATTRIBUTION, intent, flags)
    }

    companion object {
        private const val REQ_AUTH = 0x10A1
        private const val REQ_INLINE_ATTRIBUTION = 0x10A2
    }
}
