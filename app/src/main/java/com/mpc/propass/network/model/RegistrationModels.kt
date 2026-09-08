package com.mpc.propass.network.model

import com.squareup.moshi.Json
import java.io.Serializable

/**
 * Summary of the question definition attached to an attendee registration answer.
 */
data class RegistrationQuestionSummaryDto(
    @field:Json(name = "id") val id: String = "",
    @field:Json(name = "label") val label: String = "",
    @field:Json(name = "type") val type: String = "SHORT_TEXT",
    @field:Json(name = "isRequired") val isRequired: Boolean = false,
    @field:Json(name = "options") val options: List<String> = emptyList(),
    @field:Json(name = "orderIndex") val orderIndex: Int = 0
) : Serializable

/**
 * Dynamic registration answer submitted by attendee for an organizer-defined form question.
 */
data class RegistrationAnswerDto(
    @field:Json(name = "id") val id: String? = null,
    @field:Json(name = "questionId") val questionId: String = "",
    @field:Json(name = "value") val value: String = "",
    @field:Json(name = "question") val question: RegistrationQuestionSummaryDto? = null,
    @field:Json(name = "questionLabel") val questionLabel: String? = null,
    @field:Json(name = "questionType") val questionType: String? = null
) : Serializable {
    val displayLabel: String
        get() = question?.label?.takeIf { it.isNotBlank() }
            ?: questionLabel?.takeIf { it.isNotBlank() }
            ?: questionId

    val isRequired: Boolean
        get() = question?.isRequired ?: false
}

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
    @field:Json(name = "description") val description: String? = null,
    @field:Json(name = "location") val location: String? = null,
    @field:Json(name = "startDate") val startDate: String? = null,
    @field:Json(name = "endDate") val endDate: String? = null,
    @field:Json(name = "date") val date: String? = null,
    @field:Json(name = "startTime") val startTime: String? = null,
    @field:Json(name = "endTime") val endTime: String? = null,
    @field:Json(name = "maxDuration") val maxDuration: Int = 1,
    @field:Json(name = "isActive") val isActive: Boolean = true
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
 * Response data for GET /api/v1/registrations/:id.
 */
data class RegistrationDetailResponseData(
    @field:Json(name = "registration") val registration: RegistrationDto
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
