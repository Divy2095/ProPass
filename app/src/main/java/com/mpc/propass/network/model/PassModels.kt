package com.mpc.propass.network.model

import com.squareup.moshi.Json
import java.io.Serializable

/**
 * Digital pass entity returned by GET /api/v1/passes/me.
 */
data class FullDigitalPassDto(
    @field:Json(name = "id") val id: String = "",
    @field:Json(name = "passNumber") val passNumber: String = "",
    @field:Json(name = "tier") val tier: String = "STANDARD",
    @field:Json(name = "isActive") val isActive: Boolean = true,
    @field:Json(name = "isExpired") val isExpired: Boolean = false,
    @field:Json(name = "qrPayload") val qrPayload: String = "",
    @field:Json(name = "expiresAt") val expiresAt: String? = null,
    @field:Json(name = "createdAt") val createdAt: String? = null,
    @field:Json(name = "updatedAt") val updatedAt: String? = null
) : Serializable

/**
 * Verified holder profile information associated with the digital pass.
 */
data class PassHolderDto(
    @field:Json(name = "userId") val userId: String = "",
    @field:Json(name = "email") val email: String = "",
    @field:Json(name = "fullName") val fullName: String = "",
    @field:Json(name = "title") val title: String? = null,
    @field:Json(name = "organization") val organization: String? = null,
    @field:Json(name = "phone") val phone: String? = null,
    @field:Json(name = "linkedinUrl") val linkedinUrl: String? = null,
    @field:Json(name = "avatarUrl") val avatarUrl: String? = null,
    @field:Json(name = "isVerified") val isVerified: Boolean = false
) : Serializable

/**
 * Response data payload for GET /api/v1/passes/me.
 */
data class MyPassResponseData(
    @field:Json(name = "pass") val pass: FullDigitalPassDto,
    @field:Json(name = "holder") val holder: PassHolderDto
) : Serializable
