package com.mpc.propass

import java.io.Serializable

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
    val vehicleNumber: String? = null
) : Serializable
