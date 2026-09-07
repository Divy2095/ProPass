package com.mpc.propass.network.model

import com.squareup.moshi.Json
import java.io.Serializable

/**
 * Dynamic registration answer submitted by attendee for an organizer-defined form question.
 */
data class RegistrationAnswerDto(
    @field:Json(name = "questionId") val questionId: String,
    @field:Json(name = "value") val value: String
) : Serializable

/**
 * Request payload for POST /api/v1/registrations.
 */
data class CreateRegistrationRequest(
    @field:Json(name = "eventId") val eventId: String,
    @field:Json(name = "fullName") val fullName: String,
    @field:Json(name = "email") val email: String,
    @field:Json(name = "institution") val institution: String,
    @field:Json(name = "purpose") val purpose: String,
    @field:Json(name = "durationDays") val durationDays: Int,
    @field:Json(name = "vehicleNumber") val vehicleNumber: String? = null,
    @field:Json(name = "answers") val answers: List<RegistrationAnswerDto> = emptyList()
)

/**
 * Snapshot event details attached to a registration record.
 */
data class RegistrationEventDto(
    @field:Json(name = "id") val id: String = "",
    @field:Json(name = "slug") val slug: String = "",
    @field:Json(name = "title") val title: String = "",
    @field:Json(name = "overline") val overline: String? = null,
    @field:Json(name = "subtitle") val subtitle: String? = null,
    @field:Json(name = "location") val location: String = "",
    @field:Json(name = "startDate") val startDate: String = "",
    @field:Json(name = "endDate") val endDate: String = ""
) : Serializable

/**
 * Registration record entity returned by Phase 2D backend.
 */
data class RegistrationDto(
    @field:Json(name = "id") val id: String = "",
    @field:Json(name = "userId") val userId: String = "",
    @field:Json(name = "eventId") val eventId: String = "",
    @field:Json(name = "fullName") val fullName: String = "",
    @field:Json(name = "email") val email: String = "",
    @field:Json(name = "institution") val institution: String = "",
    @field:Json(name = "purpose") val purpose: String = "",
    @field:Json(name = "durationDays") val durationDays: Int = 1,
    @field:Json(name = "vehicleNumber") val vehicleNumber: String? = null,
    @field:Json(name = "status") val status: String = "CONFIRMED",
    @field:Json(name = "registeredAt") val registeredAt: String = "",
    @field:Json(name = "event") val event: RegistrationEventDto? = null,
    @field:Json(name = "answers") val answers: List<RegistrationAnswerDto> = emptyList()
) : Serializable

/**
 * Response data for POST /api/v1/registrations.
 */
data class CreateRegistrationResponseData(
    @field:Json(name = "registration") val registration: RegistrationDto
)

/**
 * Response data for GET /api/v1/registrations/my.
 */
data class MyRegistrationsResponseData(
    @field:Json(name = "registrations") val registrations: List<RegistrationDto> = emptyList(),
    @field:Json(name = "count") val count: Int? = null
)

/**
 * Bidirectional mapper between Android UI purpose strings and backend PurposeOfVisit enum strings.
 */
object RegistrationPurposeMapper {

    /**
     * Maps UI friendly strings to backend-accepted uppercase enum format.
     */
    fun toBackendPurpose(uiPurpose: String?): String {
        val trimmed = uiPurpose?.trim() ?: return "GENERAL_ATTENDEE"
        return when (trimmed.lowercase()) {
            "speaker" -> "SPEAKER"
            "sponsor", "sponsor/exhibitor", "sponsor / exhibitor", "sponsor_exhibitor" -> "SPONSOR_EXHIBITOR"
            "media", "media/press", "media / press", "media_press" -> "MEDIA_PRESS"
            else -> "GENERAL_ATTENDEE"
        }
    }

    /**
     * Maps backend uppercase enum strings to user-friendly UI presentation strings.
     */
    fun toFriendlyDisplay(backendPurpose: String?): String {
        val trimmed = backendPurpose?.trim() ?: return "General Attendee"
        return when (trimmed.uppercase()) {
            "SPEAKER" -> "Speaker"
            "SPONSOR_EXHIBITOR", "SPONSOR / EXHIBITOR", "SPONSOR" -> "Sponsor/Exhibitor"
            "MEDIA_PRESS", "MEDIA / PRESS", "MEDIA" -> "Media/Press"
            else -> "General Attendee"
        }
    }
}
