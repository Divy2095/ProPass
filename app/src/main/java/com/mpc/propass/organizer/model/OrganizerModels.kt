package com.mpc.propass.organizer.model

import com.mpc.propass.network.model.EventDto
import com.mpc.propass.network.model.FormQuestionDto
import java.io.Serializable
import java.util.UUID

/**
 * Supported field types for the ProPass Organizer Form Builder.
 */
enum class FormQuestionType(val displayName: String) : Serializable {
    SHORT_TEXT("Short Text"),
    LONG_TEXT("Long Text"),
    MULTIPLE_CHOICE("Multiple Choice"),
    CHECKBOX("Checkbox")
}

/**
 * Representation of an individual question/field in the registration form.
 */
data class FormQuestion(
    val id: String = UUID.randomUUID().toString(),
    var label: String,
    var type: FormQuestionType = FormQuestionType.SHORT_TEXT,
    var isRequired: Boolean = false,
    var options: List<String> = emptyList(),
    val isDefaultField: Boolean = false
) : Serializable

fun FormQuestion.toDto(orderIndex: Int = 0): FormQuestionDto {
    return FormQuestionDto(
        id = this.id,
        label = this.label,
        type = this.type.name,
        isRequired = this.isRequired,
        options = this.options,
        isDefaultField = this.isDefaultField,
        orderIndex = orderIndex
    )
}

fun FormQuestionDto.toFormQuestion(): FormQuestion {
    val qType = try {
        FormQuestionType.valueOf(this.type)
    } catch (_: Exception) {
        FormQuestionType.SHORT_TEXT
    }
    return FormQuestion(
        id = this.id.ifBlank { UUID.randomUUID().toString() },
        label = this.label,
        type = qType,
        isRequired = this.isRequired,
        options = this.options,
        isDefaultField = this.isDefaultField
    )
}

/**
 * Mutable draft data collected across Create Event, Form Builder, and Preview.
 */
data class OrganizerEventDraft(
    var id: String = UUID.randomUUID().toString(),
    var slug: String = "",
    var name: String = "",
    var description: String = "",
    var date: String = "",
    var startTime: String = "",
    var endTime: String = "",
    var location: String = "",
    var maxDurationDays: Int = 1,
    var qrPayload: String = "",
    val questions: MutableList<FormQuestion> = createDefaultQuestions()
) : Serializable {

    companion object {
        fun createDefaultDraft(): OrganizerEventDraft = OrganizerEventDraft()

        fun createDefaultQuestions(): MutableList<FormQuestion> = mutableListOf(
            FormQuestion(
                id = "default-full-name",
                label = "Full Name",
                type = FormQuestionType.SHORT_TEXT,
                isRequired = true,
                isDefaultField = true
            ),
            FormQuestion(
                id = "default-email",
                label = "Email Address",
                type = FormQuestionType.SHORT_TEXT,
                isRequired = true,
                isDefaultField = true
            ),
            FormQuestion(
                id = "default-phone",
                label = "Phone Number",
                type = FormQuestionType.SHORT_TEXT,
                isRequired = false,
                isDefaultField = true
            )
        )
    }

    fun toPublishedEvent(): OrganizerEvent {
        val finalSlug = slug.ifBlank {
            name.lowercase().replace(Regex("[^a-z0-9]"), "-").trim('-').ifBlank { "event" }
        }
        val finalQrPayload = qrPayload.ifBlank { "https://propass.id/event/$finalSlug" }
        return OrganizerEvent(
            id = id,
            slug = finalSlug,
            name = name,
            description = description,
            date = date,
            startTime = startTime,
            endTime = endTime,
            location = location,
            maxDurationDays = maxDurationDays,
            status = "PUBLISHED",
            questions = questions.toList(),
            qrPayload = finalQrPayload,
            publishedAt = System.currentTimeMillis(),
            attendeeCount = 0
        )
    }
}

/**
 * Published event representation used in Organizer views.
 */
data class OrganizerEvent(
    val id: String,
    val slug: String = "",
    val name: String,
    val description: String,
    val date: String,
    val startTime: String,
    val endTime: String,
    val location: String,
    val maxDurationDays: Int,
    val status: String = "PUBLISHED",
    val questions: List<FormQuestion> = emptyList(),
    val qrPayload: String = "",
    val publishedAt: Long = System.currentTimeMillis(),
    val attendeeCount: Int = 0
) : Serializable

/**
 * Convert backend EventDto to an OrganizerEvent for UI display.
 */
fun EventDto.toOrganizerEvent(
    questions: List<FormQuestion>? = null
): OrganizerEvent {
    val finalSlug = this.slug.ifBlank {
        this.title.lowercase().replace(Regex("[^a-z0-9]"), "-").trim('-').ifBlank { "event" }
    }
    val finalQr = this.qrPayload ?: "https://propass.id/event/$finalSlug"
    val displayDate = this.date ?: if (this.startDate.length >= 10) this.startDate.take(10) else ""
    val resolvedQuestions = questions ?: this.form?.questions?.map { it.toFormQuestion() } ?: OrganizerEventDraft.createDefaultQuestions()

    return OrganizerEvent(
        id = this.id,
        slug = finalSlug,
        name = this.title,
        description = this.description ?: "",
        date = displayDate,
        startTime = this.startTime ?: "",
        endTime = this.endTime ?: "",
        location = this.location,
        maxDurationDays = this.maxDuration,
        status = if (this.isActive) "PUBLISHED" else "DRAFT",
        questions = resolvedQuestions,
        qrPayload = finalQr,
        publishedAt = System.currentTimeMillis(),
        attendeeCount = this.registrationCount
    )
}
