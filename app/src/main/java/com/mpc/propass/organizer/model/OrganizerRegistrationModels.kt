package com.mpc.propass.organizer.model

import com.mpc.propass.network.model.RegistrationEventDto
import com.squareup.moshi.Json
import java.io.Serializable

/**
 * Question response with question metadata attached for organizer viewing.
 */
data class OrganizerRegistrationAnswerDto(
    @field:Json(name = "id") val id: String = "",
    @field:Json(name = "questionId") val questionId: String = "",
    @field:Json(name = "questionLabel") val questionLabel: String = "",
    @field:Json(name = "questionType") val questionType: String = "SHORT_TEXT",
    @field:Json(name = "value") val value: String = "",
    @field:Json(name = "options") val options: List<String> = emptyList(),
    @field:Json(name = "orderIndex") val orderIndex: Int = 0,
    @field:Json(name = "isDefaultField") val isDefaultField: Boolean = false
) : Serializable

/**
 * Verified identity snapshot of an attendee at time of registration or latest profile.
 */
data class AttendeeSnapshotDto(
    @field:Json(name = "id") val id: String = "",
    @field:Json(name = "fullName") val fullName: String = "",
    @field:Json(name = "email") val email: String = "",
    @field:Json(name = "institution") val institution: String = "",
    @field:Json(name = "phone") val phone: String? = null,
    @field:Json(name = "organization") val organization: String? = null,
    @field:Json(name = "title") val title: String? = null,
    @field:Json(name = "avatarUrl") val avatarUrl: String? = null
) : Serializable

/**
 * Full registration item returned to the organizer.
 */
data class OrganizerRegistrationDto(
    @field:Json(name = "id") val id: String = "",
    @field:Json(name = "eventId") val eventId: String = "",
    @field:Json(name = "userId") val userId: String = "",
    @field:Json(name = "status") val status: String = "CONFIRMED",
    @field:Json(name = "purpose") val purpose: String = "GENERAL_ATTENDEE",
    @field:Json(name = "durationDays") val durationDays: Int = 1,
    @field:Json(name = "vehicleNumber") val vehicleNumber: String? = null,
    @field:Json(name = "registeredAt") val registeredAt: String = "",
    @field:Json(name = "createdAt") val createdAt: String? = null,
    @field:Json(name = "updatedAt") val updatedAt: String? = null,
    @field:Json(name = "fullName") val fullName: String = "",
    @field:Json(name = "email") val email: String = "",
    @field:Json(name = "institution") val institution: String = "",
    @field:Json(name = "phone") val phone: String? = null,
    @field:Json(name = "attendee") val attendee: AttendeeSnapshotDto? = null,
    @field:Json(name = "answers") val answers: List<OrganizerRegistrationAnswerDto> = emptyList(),
    @field:Json(name = "event") val event: RegistrationEventDto? = null
) : Serializable

/**
 * Response data for GET /api/v1/organizer/events/:eventId/registrations.
 */
data class OrganizerRegistrationsResponseData(
    @field:Json(name = "event") val event: RegistrationEventDto? = null,
    @field:Json(name = "registrations") val registrations: List<OrganizerRegistrationDto> = emptyList(),
    @field:Json(name = "count") val count: Int = 0
)

/**
 * Response data for GET /api/v1/organizer/registrations/:registrationId.
 */
data class OrganizerRegistrationDetailResponseData(
    @field:Json(name = "registration") val registration: OrganizerRegistrationDto
)
