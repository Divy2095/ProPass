package com.mpc.propass.network.model

import com.squareup.moshi.Json
import java.io.Serializable

/**
 * Event model matching PostgreSQL Event entity returned by Phase 2C backend.
 */
data class EventDto(
    @field:Json(name = "id") val id: String = "",
    @field:Json(name = "slug") val slug: String = "",
    @field:Json(name = "title") val title: String = "",
    @field:Json(name = "overline") val overline: String? = null,
    @field:Json(name = "subtitle") val subtitle: String? = null,
    @field:Json(name = "description") val description: String? = null,
    @field:Json(name = "location") val location: String = "",
    @field:Json(name = "startDate") val startDate: String = "",
    @field:Json(name = "endDate") val endDate: String = "",
    @field:Json(name = "maxDuration") val maxDuration: Int = 1,
    @field:Json(name = "isActive") val isActive: Boolean = true,
    @field:Json(name = "createdAt") val createdAt: String? = null,
    @field:Json(name = "updatedAt") val updatedAt: String? = null
) : Serializable

/**
 * Request payload for POST /api/v1/events/validate-qr.
 */
data class ValidateQrRequest(
    @field:Json(name = "qrContent") val qrContent: String
)

/**
 * Response payload for POST /api/v1/events/validate-qr.
 */
data class ValidateQrResponseData(
    @field:Json(name = "event") val event: EventDto,
    @field:Json(name = "qrPayload") val qrPayload: String = "",
    @field:Json(name = "parsedSlug") val parsedSlug: String = ""
)

/**
 * Response payload for GET /api/v1/events/:eventId.
 */
data class EventResponseData(
    @field:Json(name = "event") val event: EventDto
)
