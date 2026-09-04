package com.mpc.propass.util

/**
 * Utility for parsing and validating ProPass Event QR URL format locally.
 *
 * Supported format:
 * https://propass.id/event/<eventId>
 * https://www.propass.id/event/<eventId>
 * with optional trailing slash and case-insensitive matching.
 */
object ProPassQrParser {

    private val PROPASS_EVENT_REGEX =
        Regex("^(?:https?)://(?:www\\.)?propass\\.id/event/([a-zA-Z0-9_-]+)/?$", RegexOption.IGNORE_CASE)

    /**
     * Extracts lowercase event slug/id from a QR content string.
     * Returns null if not matching the ProPass event URL pattern.
     */
    fun parseEventId(qrContent: String?): String? {
        if (qrContent.isNullOrBlank()) return null
        return try {
            val match = PROPASS_EVENT_REGEX.find(qrContent.trim())
            match?.groupValues?.get(1)?.lowercase()?.takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Quick boolean check whether content matches ProPass event QR pattern.
     */
    fun isProPassQr(qrContent: String?): Boolean = parseEventId(qrContent) != null
}
