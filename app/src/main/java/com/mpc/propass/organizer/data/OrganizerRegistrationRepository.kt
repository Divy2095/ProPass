package com.mpc.propass.organizer.data

import com.mpc.propass.network.NetworkClient
import com.mpc.propass.network.api.ProPassApiService
import com.mpc.propass.network.model.ApiError
import com.mpc.propass.organizer.model.OrganizerRegistrationDto
import com.mpc.propass.organizer.model.OrganizerRegistrationsResponseData
import java.io.IOException

/**
 * Repository interface for managing and viewing organizer event registrations.
 */
interface OrganizerRegistrationRepository {
    suspend fun getEventRegistrations(eventId: String): Result<OrganizerRegistrationsResponseData>
    suspend fun getRegistrationDetail(registrationId: String): Result<OrganizerRegistrationDto>
}

/**
 * Default implementation of [OrganizerRegistrationRepository] using [ProPassApiService].
 */
class OrganizerRegistrationRepositoryImpl(
    private val apiService: ProPassApiService = NetworkClient.apiService
) : OrganizerRegistrationRepository {

    private val errorAdapter = NetworkClient.moshi.adapter(ApiError::class.java)

    override suspend fun getEventRegistrations(eventId: String): Result<OrganizerRegistrationsResponseData> {
        return try {
            val response = apiService.getEventRegistrations(eventId)
            if (response.isSuccessful && response.body()?.success == true) {
                val data = response.body()?.data
                    ?: return Result.failure(IllegalStateException("Empty registrations response"))
                Result.success(data)
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string())
                    ?: response.body()?.message
                    ?: "Failed to load event registrations"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(resolveNetworkException(e))
        }
    }

    override suspend fun getRegistrationDetail(registrationId: String): Result<OrganizerRegistrationDto> {
        return try {
            val response = apiService.getRegistrationDetails(registrationId)
            if (response.isSuccessful && response.body()?.success == true) {
                val data = response.body()?.data
                    ?: return Result.failure(IllegalStateException("Empty registration detail response"))
                Result.success(data.registration)
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string())
                    ?: response.body()?.message
                    ?: "Failed to load registration details"
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
