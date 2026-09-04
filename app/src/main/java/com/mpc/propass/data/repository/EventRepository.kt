package com.mpc.propass.data.repository

import com.mpc.propass.network.NetworkClient
import com.mpc.propass.network.api.ProPassApiService
import com.mpc.propass.network.model.ApiError
import com.mpc.propass.network.model.EventDto
import com.mpc.propass.network.model.ValidateQrRequest
import com.mpc.propass.network.model.ValidateQrResponseData
import java.io.IOException

/**
 * Exceptions representing specific Event & QR validation failure scenarios.
 */
class InvalidQrException(message: String = "Invalid ProPass QR code") : Exception(message)
class EventNotFoundException(message: String = "Event not found") : Exception(message)
class EventInactiveException(message: String = "This event is no longer active or has concluded") : Exception(message)

/**
 * Repository interface for Event retrieval and QR validation operations.
 */
interface EventRepository {
    suspend fun validateQr(qrContent: String): Result<ValidateQrResponseData>
    suspend fun getEvent(eventId: String): Result<EventDto>
}

/**
 * Default implementation of [EventRepository] interacting with [ProPassApiService].
 */
class EventRepositoryImpl(
    private val apiService: ProPassApiService = NetworkClient.apiService
) : EventRepository {

    private val errorAdapter = NetworkClient.moshi.adapter(ApiError::class.java)

    override suspend fun validateQr(qrContent: String): Result<ValidateQrResponseData> {
        val trimmed = qrContent.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(InvalidQrException("QR content cannot be empty"))
        }

        return try {
            val response = apiService.validateQr(ValidateQrRequest(trimmed))
            if (response.isSuccessful && response.body()?.success == true) {
                val data = response.body()?.data
                    ?: return Result.failure(IllegalStateException("Empty validation response body"))
                Result.success(data)
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string())
                    ?: response.body()?.message
                when (response.code()) {
                    400 -> Result.failure(InvalidQrException(errorMsg ?: "Invalid ProPass QR code"))
                    404 -> Result.failure(EventNotFoundException(errorMsg ?: "Event not found"))
                    410 -> Result.failure(EventInactiveException(errorMsg ?: "This event is no longer active or has concluded"))
                    else -> Result.failure(Exception(errorMsg ?: "Failed to validate QR code (${response.code()})"))
                }
            }
        } catch (e: Exception) {
            Result.failure(resolveNetworkException(e))
        }
    }

    override suspend fun getEvent(eventId: String): Result<EventDto> {
        val trimmed = eventId.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("Event ID cannot be empty"))
        }

        return try {
            val response = apiService.getEvent(trimmed)
            if (response.isSuccessful && response.body()?.success == true) {
                val data = response.body()?.data
                    ?: return Result.failure(IllegalStateException("Empty event response body"))
                Result.success(data.event)
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string())
                    ?: response.body()?.message
                when (response.code()) {
                    404 -> Result.failure(EventNotFoundException(errorMsg ?: "Event not found"))
                    410 -> Result.failure(EventInactiveException(errorMsg ?: "This event is no longer active or has concluded"))
                    else -> Result.failure(Exception(errorMsg ?: "Failed to retrieve event (${response.code()})"))
                }
            }
        } catch (e: Exception) {
            Result.failure(resolveNetworkException(e))
        }
    }

    private fun parseErrorMessage(errorJson: String?): String? {
        if (errorJson.isNullOrBlank()) return null
        return try {
            val apiError = errorAdapter.fromJson(errorJson)
            apiError?.message ?: apiError?.error
        } catch (_: Exception) {
            null
        }
    }

    private fun resolveNetworkException(e: Exception): Exception {
        return if (e is IOException) {
            IOException("Unable to connect to ProPass server. Please check your network connection.", e)
        } else {
            e
        }
    }
}
