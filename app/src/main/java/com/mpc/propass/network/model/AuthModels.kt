package com.mpc.propass.network.model

import com.squareup.moshi.Json

/**
 * Request payload for user login (POST /api/v1/auth/login).
 */
data class LoginRequest(
    @field:Json(name = "email") val email: String,
    @field:Json(name = "password") val password: String
)

/**
 * Request payload for user registration (POST /api/v1/auth/register).
 */
data class RegisterRequest(
    @field:Json(name = "email") val email: String,
    @field:Json(name = "password") val password: String
)

/**
 * Request payload for refreshing access tokens or logging out (POST /api/v1/auth/refresh, POST /api/v1/auth/logout).
 */
data class RefreshTokenRequest(
    @field:Json(name = "refreshToken") val refreshToken: String
)

/**
 * Canonical user roles supported by the ProPass platform.
 */
enum class UserRole {
    ATTENDEE,
    ORGANIZER;

    companion object {
        fun fromString(value: String?): UserRole {
            return when (value?.uppercase()) {
                ORGANIZER.name -> ORGANIZER
                else -> ATTENDEE
            }
        }
    }
}

/**
 * User account information returned by the authentication endpoints.
 */
data class AuthUserDto(
    @field:Json(name = "id") val id: String = "",
    @field:Json(name = "email") val email: String = "",
    @field:Json(name = "role") val role: String? = UserRole.ATTENDEE.name,
    @field:Json(name = "createdAt") val createdAt: String? = null
) {
    val isOrganizer: Boolean
        get() = role?.equals(UserRole.ORGANIZER.name, ignoreCase = true) == true

    val userRole: UserRole
        get() = UserRole.fromString(role)
}

/**
 * Access and refresh token pair returned upon successful authentication or token refresh.
 */
data class AuthTokensDto(
    @field:Json(name = "accessToken") val accessToken: String = "",
    @field:Json(name = "refreshToken") val refreshToken: String = "",
    @field:Json(name = "expiresIn") val expiresIn: String? = null
)

/**
 * Data payload returned by POST /api/v1/auth/register and POST /api/v1/auth/login.
 */
data class AuthResponseData(
    @field:Json(name = "user") val user: AuthUserDto,
    @field:Json(name = "tokens") val tokens: AuthTokensDto
)

/**
 * Data payload returned by POST /api/v1/auth/refresh.
 */
data class RefreshResponseData(
    @field:Json(name = "tokens") val tokens: AuthTokensDto
)

/**
 * Data payload returned by GET /api/v1/auth/me.
 */
data class MeResponseData(
    @field:Json(name = "user") val user: AuthUserDto
)
