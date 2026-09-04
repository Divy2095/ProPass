package com.mpc.propass

import com.mpc.propass.util.ProPassQrParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProPassQrValidationTest {

    @Test
    fun testValidProPassEventUrls() {
        assertEquals("techconf-2024", ProPassQrParser.parseEventId("https://propass.id/event/techconf-2024"))
        assertEquals("google-office-visit", ProPassQrParser.parseEventId("https://propass.id/event/google-office-visit"))
        assertEquals("android-conf-2026", ProPassQrParser.parseEventId("https://propass.id/event/android-conf-2026"))
        assertEquals("techconf-2024", ProPassQrParser.parseEventId("https://propass.id/event/techconf-2024/"))
        assertEquals("techconf-2024", ProPassQrParser.parseEventId("http://propass.id/event/techconf-2024"))
        assertEquals("techconf-2024", ProPassQrParser.parseEventId("https://www.propass.id/event/techconf-2024"))
        assertEquals("techconf-2024", ProPassQrParser.parseEventId("HTTPS://PROPASS.ID/EVENT/TECHCONF-2024"))

        assertTrue(ProPassQrParser.isProPassQr("https://propass.id/event/techconf-2024"))
        assertTrue(ProPassQrParser.isProPassQr("https://www.propass.id/event/my-cool-event_123/"))
    }

    @Test
    fun testInvalidUrlsAndContent() {
        assertNull(ProPassQrParser.parseEventId("https://google.com"))
        assertNull(ProPassQrParser.parseEventId("https://youtube.com"))
        assertNull(ProPassQrParser.parseEventId("https://chatgpt.com/c/6a8fce3b-6040-83ee-8124-3b85f93cd95f"))
        assertNull(ProPassQrParser.parseEventId("https://example.com/event/techconf-2024"))
        assertNull(ProPassQrParser.parseEventId("https://propass.id/other/techconf-2024"))
        assertNull(ProPassQrParser.parseEventId("https://propass.id/event/"))
        assertNull(ProPassQrParser.parseEventId("https://propass.id/event"))
        assertNull(ProPassQrParser.parseEventId("random string"))
        assertNull(ProPassQrParser.parseEventId("WIFI:T:WPA;S:MyNetwork;P:MyPassword;;"))
        assertNull(ProPassQrParser.parseEventId("tel:+1234567890"))
        assertNull(ProPassQrParser.parseEventId("mailto:test@propass.id"))
        assertNull(ProPassQrParser.parseEventId(""))
        assertNull(ProPassQrParser.parseEventId(null))

        assertFalse(ProPassQrParser.isProPassQr("https://google.com"))
        assertFalse(ProPassQrParser.isProPassQr(null))
        assertFalse(ProPassQrParser.isProPassQr(""))
    }
}
