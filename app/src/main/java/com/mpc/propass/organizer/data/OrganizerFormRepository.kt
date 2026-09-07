package com.mpc.propass.organizer.data

import com.mpc.propass.network.NetworkClient
import com.mpc.propass.network.api.ProPassApiService
import com.mpc.propass.network.model.ApiError
import com.mpc.propass.network.model.FormQuestionDto
import com.mpc.propass.network.model.RegistrationFormDto
import com.mpc.propass.network.model.SaveFormRequest
import java.io.IOException

/**
 * Repository interface for retrieving and saving organizer event registration forms.
 */
interface OrganizerFormRepository {
    suspend fun getForm(eventId: String): Result<RegistrationFormDto>
    suspend fun saveForm(eventId: String, questions: List<FormQuestionDto>): Result<RegistrationFormDto>
}

/**
 * Default implementation of [OrganizerFormRepository] communicating with [ProPassApiService].
 */
class OrganizerFormRepositoryImpl(
    private val apiService: ProPassApiService = NetworkClient.apiService
) : OrganizerFormRepository {

    private val errorAdapter = NetworkClient.moshi.adapter(ApiError::class.java)

    override suspend fun getForm(eventId: String): Result<RegistrationFormDto> {
        val cleanId = eventId.trim()
        if (cleanId.isEmpty()) {
            return Result.failure(IllegalArgumentException("Event identifier cannot be empty"))
        }

        return try {
            val response = apiService.getEventForm(cleanId)
            if (response.isSuccessful && response.body()?.success == true) {
                val data = response.body()?.data
                    ?: return Result.failure(IllegalStateException("Empty response body"))
                Result.success(data.form)
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string())
                    ?: response.body()?.message
                    ?: "Failed to retrieve registration form"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(resolveNetworkException(e))
        }
    }

    override suspend fun saveForm(eventId: String, questions: List<FormQuestionDto>): Result<RegistrationFormDto> {
        val cleanId = eventId.trim()
        if (cleanId.isEmpty()) {
            return Result.failure(IllegalArgumentException("Event identifier cannot be empty"))
        }

        return try {
            val response = apiService.saveEventForm(cleanId, SaveFormRequest(questions))
            if (response.isSuccessful && response.body()?.success == true) {
                val data = response.body()?.data
                    ?: return Result.failure(IllegalStateException("Empty response body"))
                Result.success(data.form)
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string())
                    ?: response.body()?.message
                    ?: "Failed to save registration form"
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
