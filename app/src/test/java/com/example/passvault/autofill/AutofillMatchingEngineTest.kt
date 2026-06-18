package com.example.passvault.autofill

import com.example.passvault.data.model.PasswordEntry
import com.example.passvault.data.repository.PasswordRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AutofillMatchingEngineTest {

    private val repository = mockk<PasswordRepository>(relaxed = true)
    private val engine = AutofillMatchingEngine(repository)

    @Test
    fun `browser context matches by webDomain`() = runBlocking {
        val match = passwordEntry(id = 1, website = "github.com")
        coEvery { repository.findByWebsite("github.com") } returns listOf(match)

        val ctx = AutofillContext(
            packageName = "com.android.chrome",
            webDomain = "github.com",
            usernameFieldId = null,
            passwordFieldId = null,
            usernameValue = null,
            passwordValue = null
        )
        val result = engine.findMatches(ctx)
        assertEquals(listOf(match), result)
    }

    @Test
    fun `non-browser context matches by package name`() = runBlocking {
        val match = passwordEntry(id = 2, appPackageName = "com.twitter.android")
        coEvery { repository.findByPackageName("com.twitter.android") } returns listOf(match)

        val ctx = AutofillContext(
            packageName = "com.twitter.android",
            webDomain = null,
            usernameFieldId = null,
            passwordFieldId = null,
            usernameValue = null,
            passwordValue = null
        )
        val result = engine.findMatches(ctx)
        assertEquals(listOf(match), result)
    }

    @Test
    fun `browser without webDomain returns empty`() = runBlocking {
        val ctx = AutofillContext(
            packageName = "com.android.chrome",
            webDomain = null,
            usernameFieldId = null,
            passwordFieldId = null,
            usernameValue = null,
            passwordValue = null
        )
        val result = engine.findMatches(ctx)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `recognises common browser packages`() {
        assertTrue(engine.isBrowser("com.android.chrome"))
        assertTrue(engine.isBrowser("org.mozilla.firefox"))
        assertTrue(engine.isBrowser("com.microsoft.emmx"))
        assertTrue(!engine.isBrowser("com.twitter.android"))
    }

    private fun passwordEntry(
        id: Long = 0,
        title: String = "Test",
        username: String = "user",
        website: String = "",
        appPackageName: String = ""
    ) = PasswordEntry(
        id = id,
        title = title,
        username = username,
        encryptedPassword = ByteArray(0),
        passwordIv = ByteArray(0),
        website = website,
        appPackageName = appPackageName
    )
}
