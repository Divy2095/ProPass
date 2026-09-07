package com.mpc.propass.organizer.model

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

/**
 * Mutable draft data collected across Create Event, Form Builder, and Preview.
 */
data class OrganizerEventDraft(
    var id: String = UUID.randomUUID().toString(),
    var name: String = "",
    var description: String = "",
    var date: String = "",
    var startTime: String = "",
    var endTime: String = "",
    var location: String = "",
    var maxDurationDays: Int = 1,
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
        val slug = name.lowercase().replace(Regex("[^a-z0-9]"), "-").trim('-').ifBlank { "event" }
        return OrganizerEvent(
            id = id,
            name = name,
            description = description,
            date = date,
            startTime = startTime,
            endTime = endTime,
            location = location,
            maxDurationDays = maxDurationDays,
            status = "PUBLISHED",
            questions = questions.toList(),
            qrPayload = "https://propass.id/event/$slug",
            publishedAt = System.currentTimeMillis(),
            attendeeCount = 0
        )
    }
}

/**
 * Published local mock event stored in OrganizerEventStore.
 */
data class OrganizerEvent(
    val id: String,
    val name: String,
    val description: String,
    val date: String,
    val startTime: String,
    val endTime: String,
    val location: String,
    val maxDurationDays: Int,
    val status: String = "PUBLISHED",
    val questions: List<FormQuestion>,
    val qrPayload: String,
    val publishedAt: Long = System.currentTimeMillis(),
    val attendeeCount: Int = 0
) : Serializable
