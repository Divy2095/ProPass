package com.mpc.propass

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProPassQrValidationTest {

    private fun parseProPassEventId(qrContent: String): String? {
        if (qrContent.isBlank()) return null
        return try {
            val pattern = Regex("^(?:https?)://(?:www\\.)?propass\\.id/event/([a-zA-Z0-9_-]+)/?$", RegexOption.IGNORE_CASE)
            val match = pattern.find(qrContent.trim())
            match?.groupValues?.get(1)?.lowercase()?.takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            null
        }
    }

    @Test
    fun testValidProPassEventUrls() {
        assertEquals("techconf-2024", parseProPassEventId("https://propass.id/event/techconf-2024"))
        assertEquals("google-office-visit", parseProPassEventId("https://propass.id/event/google-office-visit"))
        assertEquals("android-conf-2026", parseProPassEventId("https://propass.id/event/android-conf-2026"))
        assertEquals("techconf-2024", parseProPassEventId("https://propass.id/event/techconf-2024/"))
        assertEquals("techconf-2024", parseProPassEventId("http://propass.id/event/techconf-2024"))
        assertEquals("techconf-2024", parseProPassEventId("https://www.propass.id/event/techconf-2024"))
        assertEquals("techconf-2024", parseProPassEventId("HTTPS://PROPASS.ID/EVENT/TECHCONF-2024"))
    }

    @Test
    fun testInvalidUrlsAndContent() {
        assertNull(parseProPassEventId("https://google.com"))
        assertNull(parseProPassEventId("https://youtube.com"))
        assertNull(parseProPassEventId("https://chatgpt.com/c/6a8fce3b-6040-83ee-8124-3b85f93cd95f"))
        assertNull(parseProPassEventId("https://example.com/event/techconf-2024"))
        assertNull(parseProPassEventId("https://propass.id/other/techconf-2024"))
        assertNull(parseProPassEventId("https://propass.id/event/"))
        assertNull(parseProPassEventId("https://propass.id/event"))
        assertNull(parseProPassEventId("random string"))
        assertNull(parseProPassEventId("WIFI:T:WPA;S:MyNetwork;P:MyPassword;;"))
        assertNull(parseProPassEventId("tel:+1234567890"))
        assertNull(parseProPassEventId("mailto:test@propass.id"))
        assertNull(parseProPassEventId(""))
    }
}
