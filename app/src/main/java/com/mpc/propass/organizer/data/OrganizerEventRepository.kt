package com.mpc.propass.organizer.data

import com.mpc.propass.network.NetworkClient
import com.mpc.propass.network.api.ProPassApiService
import com.mpc.propass.network.model.ApiError
import com.mpc.propass.network.model.CreateEventRequest
import com.mpc.propass.network.model.EventDto
import java.io.IOException

/**
 * Repository interface for organizer-specific event creation and management.
 */
interface OrganizerEventRepository {
    suspend fun createEvent(request: CreateEventRequest): Result<EventDto>
    suspend fun getMyEvents(): Result<List<EventDto>>
}

/**
 * Default implementation of [OrganizerEventRepository] using [ProPassApiService].
 */
class OrganizerEventRepositoryImpl(
    private val apiService: ProPassApiService = NetworkClient.apiService
) : OrganizerEventRepository {

    private val errorAdapter = NetworkClient.moshi.adapter(ApiError::class.java)

    override suspend fun createEvent(request: CreateEventRequest): Result<EventDto> {
        return try {
            val response = apiService.createEvent(request)
            if (response.isSuccessful && response.body()?.success == true) {
                val data = response.body()?.data
                    ?: return Result.failure(IllegalStateException("Empty create event response body"))
                Result.success(data.event)
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string())
                    ?: response.body()?.message
                    ?: "Failed to create event"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(resolveNetworkException(e))
        }
    }

    override suspend fun getMyEvents(): Result<List<EventDto>> {
        return try {
            val response = apiService.getOrganizerEvents()
            if (response.isSuccessful && response.body()?.success == true) {
                val data = response.body()?.data
                Result.success(data?.events ?: emptyList())
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string())
                    ?: response.body()?.message
                    ?: "Failed to fetch organizer events"
                Result.failure(Exception(errorMsg))
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
