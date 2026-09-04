package com.mpc.propass.network.model

import com.squareup.moshi.Json

/**
 * Detailed validation error item for individual form fields.
 */
data class ApiErrorDetail(
    @field:Json(name = "field") val field: String? = null,
    @field:Json(name = "message") val message: String? = null
)

/**
 * Structured API error response representation for non-2xx responses.
 */
data class ApiError(
    @field:Json(name = "success") val success: Boolean = false,
    @field:Json(name = "error") val error: String? = null,
    @field:Json(name = "message") val message: String? = null,
    @field:Json(name = "statusCode") val statusCode: Int? = null,
    @field:Json(name = "details") val details: List<ApiErrorDetail>? = null,
    @field:Json(name = "timestamp") val timestamp: String? = null
)
