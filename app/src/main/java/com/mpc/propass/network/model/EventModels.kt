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
    @field:Json(name = "date") val date: String? = null,
    @field:Json(name = "startTime") val startTime: String? = null,
    @field:Json(name = "endTime") val endTime: String? = null,
    @field:Json(name = "location") val location: String = "",
    @field:Json(name = "startDate") val startDate: String = "",
    @field:Json(name = "endDate") val endDate: String = "",
    @field:Json(name = "maxDuration") val maxDuration: Int = 1,
    @field:Json(name = "isActive") val isActive: Boolean = true,
    @field:Json(name = "organizerId") val organizerId: String? = null,
    @field:Json(name = "qrPayload") val qrPayload: String? = null,
    @field:Json(name = "form") val form: RegistrationFormDto? = null,
    @field:Json(name = "registrationCount") val registrationCount: Int = 0,
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

/**
 * Request payload for POST /api/v1/events (Phase 4B).
 */
data class CreateEventRequest(
    @field:Json(name = "name") val name: String,
    @field:Json(name = "description") val description: String? = null,
    @field:Json(name = "date") val date: String,
    @field:Json(name = "startTime") val startTime: String? = null,
    @field:Json(name = "endTime") val endTime: String? = null,
    @field:Json(name = "location") val location: String,
    @field:Json(name = "maxDuration") val maxDuration: Int = 1,
    @field:Json(name = "slug") val slug: String? = null
)

/**
 * Response payload for POST /api/v1/events.
 */
data class CreateEventResponseData(
    @field:Json(name = "event") val event: EventDto
)

/**
 * Response payload for GET /api/v1/organizer/events.
 */
data class OrganizerEventsResponseData(
    @field:Json(name = "events") val events: List<EventDto> = emptyList()
)

/**
 * Representation of a registration form question returned by backend.
 */
data class FormQuestionDto(
    @field:Json(name = "id") val id: String = "",
    @field:Json(name = "label") val label: String = "",
    @field:Json(name = "type") val type: String = "SHORT_TEXT",
    @field:Json(name = "isRequired") val isRequired: Boolean = false,
    @field:Json(name = "options") val options: List<String> = emptyList(),
    @field:Json(name = "isDefaultField") val isDefaultField: Boolean = false,
    @field:Json(name = "orderIndex") val orderIndex: Int = 0
) : Serializable

/**
 * Representation of the complete registration form attached to an event.
 */
data class RegistrationFormDto(
    @field:Json(name = "id") val id: String = "",
    @field:Json(name = "eventId") val eventId: String = "",
    @field:Json(name = "questions") val questions: List<FormQuestionDto> = emptyList()
) : Serializable

/**
 * Request payload for PUT /api/v1/events/:eventId/form.
 */
data class SaveFormRequest(
    @field:Json(name = "questions") val questions: List<FormQuestionDto>
)

/**
 * Response payload for PUT /api/v1/events/:eventId/form and GET /api/v1/events/:eventId/form.
 */
data class SaveFormResponseData(
    @field:Json(name = "form") val form: RegistrationFormDto
)
