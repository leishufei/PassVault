package com.example.passvault.autofill

import android.app.assist.AssistStructure
import android.os.Build
import android.text.InputType
import android.view.View
import android.view.autofill.AutofillId
import android.view.autofill.AutofillValue
import androidx.annotation.RequiresApi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@RequiresApi(Build.VERSION_CODES.O)
class AutofillDataParser @Inject constructor() {

    fun parse(structure: AssistStructure): AutofillContext {
        val packageName = structure.activityComponent?.packageName.orEmpty()
        var webDomain: String? = null
        var usernameFieldId: AutofillId? = null
        var passwordFieldId: AutofillId? = null
        var usernameValue: String? = null
        var passwordValue: String? = null

        for (i in 0 until structure.windowNodeCount) {
            val rootView = structure.getWindowNodeAt(i).rootViewNode
            traverse(rootView) { node ->
                if (webDomain == null && !node.webDomain.isNullOrBlank()) {
                    webDomain = normalizeDomain(node.webDomain)
                }
                val fieldKind = classifyField(node)
                if (fieldKind != FieldKind.UNKNOWN && node.autofillId != null) {
                    when (fieldKind) {
                        FieldKind.USERNAME, FieldKind.EMAIL -> {
                            if (usernameFieldId == null) {
                                usernameFieldId = node.autofillId
                                usernameValue = readText(node.autofillValue)
                                    ?: node.text?.toString()
                            }
                        }
                        FieldKind.PASSWORD -> {
                            if (passwordFieldId == null) {
                                passwordFieldId = node.autofillId
                                passwordValue = readText(node.autofillValue)
                                    ?: node.text?.toString()
                            }
                        }
                        else -> Unit
                    }
                }
            }
        }

        return AutofillContext(
            packageName = packageName,
            webDomain = webDomain,
            usernameFieldId = usernameFieldId,
            passwordFieldId = passwordFieldId,
            usernameValue = usernameValue,
            passwordValue = passwordValue
        )
    }

    private fun traverse(node: AssistStructure.ViewNode, visitor: (AssistStructure.ViewNode) -> Unit) {
        visitor(node)
        for (i in 0 until node.childCount) {
            traverse(node.getChildAt(i), visitor)
        }
    }

    private fun classifyField(node: AssistStructure.ViewNode): FieldKind {
        val hints = node.autofillHints
        if (!hints.isNullOrEmpty()) {
            for (hint in hints) {
                when (hint.lowercase()) {
                    View.AUTOFILL_HINT_PASSWORD -> return FieldKind.PASSWORD
                    View.AUTOFILL_HINT_USERNAME -> return FieldKind.USERNAME
                    View.AUTOFILL_HINT_EMAIL_ADDRESS -> return FieldKind.EMAIL
                }
                if (hint.contains("password", ignoreCase = true)) return FieldKind.PASSWORD
                if (hint.contains("username", ignoreCase = true)) return FieldKind.USERNAME
                if (hint.contains("email", ignoreCase = true)) return FieldKind.EMAIL
            }
        }

        if (node.className?.contains("EditText", ignoreCase = true) != true &&
            node.htmlInfo?.tag?.equals("input", ignoreCase = true) != true
        ) {
            return FieldKind.UNKNOWN
        }

        val htmlType = node.htmlInfo?.attributes?.firstOrNull { it.first.equals("type", ignoreCase = true) }?.second
        when (htmlType?.lowercase()) {
            "password" -> return FieldKind.PASSWORD
            "email" -> return FieldKind.EMAIL
            "text", "tel" -> {
                if (matchesUsernameHint(node)) return FieldKind.USERNAME
            }
        }

        val inputType = node.inputType
        if (inputType and InputType.TYPE_MASK_CLASS == InputType.TYPE_CLASS_TEXT) {
            val variation = inputType and InputType.TYPE_MASK_VARIATION
            when (variation) {
                InputType.TYPE_TEXT_VARIATION_PASSWORD,
                InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
                InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD -> return FieldKind.PASSWORD
                InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
                InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS -> return FieldKind.EMAIL
            }
            if (matchesUsernameHint(node)) return FieldKind.USERNAME
        }

        return FieldKind.UNKNOWN
    }

    private fun matchesUsernameHint(node: AssistStructure.ViewNode): Boolean {
        val candidates = buildList {
            node.idEntry?.let { add(it) }
            node.hint?.let { add(it.toString()) }
            node.contentDescription?.let { add(it.toString()) }
            node.htmlInfo?.attributes?.forEach { add(it.second.orEmpty()) }
        }
        return candidates.any { value ->
            USERNAME_KEYWORDS.any { value.contains(it, ignoreCase = true) }
        }
    }

    private fun readText(value: AutofillValue?): String? {
        if (value == null) return null
        return if (value.isText) value.textValue?.toString() else null
    }

    private fun normalizeDomain(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        var domain = raw.trim().lowercase()
        domain = domain.removePrefix("https://").removePrefix("http://")
        domain = domain.substringBefore('/')
        domain = domain.substringBefore(':')
        return domain.ifBlank { null }
    }

    private enum class FieldKind { UNKNOWN, USERNAME, EMAIL, PASSWORD }

    companion object {
        private val USERNAME_KEYWORDS = listOf(
            "user", "login", "account", "email", "phone", "mobile", "id"
        )
    }
}
