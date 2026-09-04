package com.mpc.propass.network.model

import com.squareup.moshi.Json

/**
 * Standard API response envelope returned by the ProPass Fastify backend.
 *
 * @param T The type of data contained in the payload.
 * @property success Indicates whether the operation succeeded.
 * @property message Human-readable message or description.
 * @property data The response payload, present on successful responses.
 * @property error Error type or title (e.g. "Bad Request", "Conflict", "Not Found").
 * @property statusCode HTTP status code returned in the JSON envelope.
 * @property timestamp ISO 8601 timestamp string from the backend.
 */
data class ApiResponse<T>(
    @field:Json(name = "success") val success: Boolean = false,
    @field:Json(name = "message") val message: String? = null,
    @field:Json(name = "data") val data: T? = null,
    @field:Json(name = "error") val error: String? = null,
    @field:Json(name = "statusCode") val statusCode: Int? = null,
    @field:Json(name = "timestamp") val timestamp: String? = null
)
