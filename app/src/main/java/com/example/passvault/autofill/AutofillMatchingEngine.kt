package com.example.passvault.autofill

import com.example.passvault.data.model.PasswordEntry
import com.example.passvault.data.repository.PasswordRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AutofillMatchingEngine @Inject constructor(
    private val repository: PasswordRepository
) {

    suspend fun findMatches(context: AutofillContext): List<PasswordEntry> {
        val isBrowser = context.packageName in BROWSER_PACKAGES
        return when {
            isBrowser && !context.webDomain.isNullOrBlank() ->
                repository.findByWebsite(context.webDomain)
            !isBrowser && context.packageName.isNotBlank() ->
                repository.findByPackageName(context.packageName)
            else -> emptyList()
        }
    }

    fun isBrowser(packageName: String): Boolean = packageName in BROWSER_PACKAGES

    companion object {
        private val BROWSER_PACKAGES = setOf(
            "com.android.chrome",
            "com.chrome.beta",
            "com.chrome.dev",
            "com.chrome.canary",
            "org.mozilla.firefox",
            "org.mozilla.firefox_beta",
            "org.mozilla.fennec_aurora",
            "org.mozilla.focus",
            "com.microsoft.emmx",
            "com.opera.browser",
            "com.opera.mini.native",
            "com.brave.browser",
            "com.duckduckgo.mobile.android",
            "com.sec.android.app.sbrowser",
            "com.kiwibrowser.browser",
            "com.vivaldi.browser",
            "com.UCMobile.intl",
            "com.tencent.mtt",
            "com.baidu.browser.apps",
            "com.huawei.browser",
            "com.qihoo.browser"
        )
    }
}
