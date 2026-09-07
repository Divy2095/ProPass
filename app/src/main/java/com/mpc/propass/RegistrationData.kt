package com.mpc.propass

import java.io.Serializable

/**
 * Answer for an organizer-defined dynamic form question collected during SmartForm registration.
 */
data class RegistrationAnswerData(
    val questionId: String,
    val questionLabel: String = "",
    val value: String
) : Serializable

/**
 * Local data model representing user's event registration payload.
 */
data class RegistrationData(
    val eventId: String,
    val eventName: String,
    val fullName: String,
    val email: String,
    val institution: String,
    val purpose: String,
    val durationDays: Int,
    val vehicleNumber: String? = null,
    val answers: List<RegistrationAnswerData> = emptyList()
) : Serializable
