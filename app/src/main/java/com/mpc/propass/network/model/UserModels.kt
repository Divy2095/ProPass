package com.mpc.propass.network.model

import com.squareup.moshi.Json

/**
 * User Profile entity returned by the backend (Phase 2B).
 */
data class UserProfileDto(
    @field:Json(name = "id") val id: String? = null,
    @field:Json(name = "userId") val userId: String? = null,
    @field:Json(name = "fullName") val fullName: String = "",
    @field:Json(name = "title") val title: String? = null,
    @field:Json(name = "organization") val organization: String? = null,
    @field:Json(name = "phone") val phone: String? = null,
    @field:Json(name = "linkedinUrl") val linkedinUrl: String? = null,
    @field:Json(name = "avatarUrl") val avatarUrl: String? = null,
    @field:Json(name = "isVerified") val isVerified: Boolean = false,
    @field:Json(name = "completionScore") val completionScore: Int = 0,
    @field:Json(name = "createdAt") val createdAt: String? = null,
    @field:Json(name = "updatedAt") val updatedAt: String? = null
)

/**
 * Response payload for GET /api/v1/users/profile and PUT /api/v1/users/profile.
 */
data class UserProfileResponseData(
    @field:Json(name = "user") val user: AuthUserDto,
    @field:Json(name = "profile") val profile: UserProfileDto? = null
)

/**
 * Request payload for PUT /api/v1/users/profile.
 */
data class UpdateProfileRequest(
    @field:Json(name = "fullName") val fullName: String? = null,
    @field:Json(name = "title") val title: String? = null,
    @field:Json(name = "organization") val organization: String? = null,
    @field:Json(name = "phone") val phone: String? = null,
    @field:Json(name = "linkedinUrl") val linkedinUrl: String? = null,
    @field:Json(name = "avatarUrl") val avatarUrl: String? = null
)
