package com.mpc.propass.network.model

import com.squareup.moshi.Json

/**
 * Digital Pass preview model nested within GET /api/v1/dashboard.
 */
data class DashboardPassDto(
    @field:Json(name = "id") val id: String = "",
    @field:Json(name = "passNumber") val passNumber: String = "",
    @field:Json(name = "tier") val tier: String = "STANDARD",
    @field:Json(name = "isActive") val isActive: Boolean = true,
    @field:Json(name = "qrPayload") val qrPayload: String = "",
    @field:Json(name = "expiresAt") val expiresAt: String? = null
)

/**
 * Recent event registration activity nested within GET /api/v1/dashboard.
 */
data class DashboardRecentActivityDto(
    @field:Json(name = "registrationId") val registrationId: String = "",
    @field:Json(name = "eventId") val eventId: String = "",
    @field:Json(name = "eventSlug") val eventSlug: String = "",
    @field:Json(name = "eventTitle") val eventTitle: String = "",
    @field:Json(name = "eventOverline") val eventOverline: String? = null,
    @field:Json(name = "eventSubtitle") val eventSubtitle: String? = null,
    @field:Json(name = "eventLocation") val eventLocation: String = "",
    @field:Json(name = "eventStartDate") val eventStartDate: String? = null,
    @field:Json(name = "purpose") val purpose: String? = null,
    @field:Json(name = "durationDays") val durationDays: Int = 1,
    @field:Json(name = "status") val status: String = "",
    @field:Json(name = "registeredAt") val registeredAt: String = ""
)

/**
 * Aggregated dashboard payload returned by GET /api/v1/dashboard.
 */
data class DashboardResponseData(
    @field:Json(name = "greeting") val greeting: String = "",
    @field:Json(name = "user") val user: AuthUserDto,
    @field:Json(name = "profile") val profile: UserProfileDto? = null,
    @field:Json(name = "pass") val pass: DashboardPassDto? = null,
    @field:Json(name = "recentActivity") val recentActivity: List<DashboardRecentActivityDto> = emptyList()
)
