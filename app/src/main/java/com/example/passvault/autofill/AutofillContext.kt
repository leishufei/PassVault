package com.example.passvault.autofill

import android.view.autofill.AutofillId

data class AutofillFieldId(
    val autofillId: AutofillId,
    val isUsername: Boolean,
    val isPassword: Boolean,
    val isEmail: Boolean
)

data class AutofillContext(
    val packageName: String,
    val webDomain: String?,
    val usernameFieldId: AutofillId?,
    val passwordFieldId: AutofillId?,
    val usernameValue: String?,
    val passwordValue: String?
) {
    val hasFillableFields: Boolean
        get() = usernameFieldId != null || passwordFieldId != null

    val hasSavableValues: Boolean
        get() = !passwordValue.isNullOrEmpty() && !usernameValue.isNullOrEmpty()

    val isBrowser: Boolean
        get() = !webDomain.isNullOrBlank()
}
