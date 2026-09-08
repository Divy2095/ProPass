package com.mpc.propass.data.repository

import com.mpc.propass.network.NetworkClient
import com.mpc.propass.network.api.ProPassApiService
import com.mpc.propass.network.model.ApiError
import com.mpc.propass.network.model.CreateRegistrationRequest
import com.mpc.propass.network.model.CreateRegistrationResponseData
import com.mpc.propass.network.model.RegistrationAnswerDto
import com.mpc.propass.network.model.RegistrationDto
import java.io.IOException

/**
 * Exceptions representing specific Event Registration failure scenarios.
 */
class DuplicateRegistrationException(message: String = "You are already registered for this event") : Exception(message)
class RegistrationValidationException(message: String = "Invalid registration data") : Exception(message)
class RegistrationAuthException(message: String = "Authentication required. Please sign in.") : Exception(message)
class RegistrationNotFoundException(message: String = "Event not found") : Exception(message)

/**
 * Repository interface for Event Registration operations.
 */
interface RegistrationRepository {
    suspend fun createRegistration(
        eventId: String,
        fullName: String,
        email: String,
        institution: String,
        purpose: String,
        durationDays: Int,
        vehicleNumber: String? = null,
        answers: List<RegistrationAnswerDto> = emptyList()
    ): Result<CreateRegistrationResponseData>

    suspend fun getMyRegistrations(): Result<List<RegistrationDto>>

    suspend fun getRegistrationById(registrationId: String): Result<RegistrationDto>
}

/**
 * Default implementation of [RegistrationRepository] interacting with [ProPassApiService].
 */
class RegistrationRepositoryImpl(
    private val apiService: ProPassApiService = NetworkClient.apiService
) : RegistrationRepository {

    private val errorAdapter = NetworkClient.moshi.adapter(ApiError::class.java)

    override suspend fun createRegistration(
        eventId: String,
        fullName: String,
        email: String,
        institution: String,
        purpose: String,
        durationDays: Int,
        vehicleNumber: String?,
        answers: List<RegistrationAnswerDto>
    ): Result<CreateRegistrationResponseData> {
        val trimmedEventId = eventId.trim()
        val trimmedFullName = fullName.trim()
        val trimmedEmail = email.trim()
        val trimmedInstitution = institution.trim()
        val trimmedPurpose = purpose.trim()
        val trimmedVehicle = vehicleNumber?.trim()?.takeIf { it.isNotEmpty() }

        if (trimmedEventId.isEmpty()) {
            return Result.failure(RegistrationValidationException("Event identifier cannot be empty"))
        }
        if (trimmedFullName.isEmpty()) {
            return Result.failure(RegistrationValidationException("Full name cannot be empty"))
        }
        if (trimmedEmail.isEmpty()) {
            return Result.failure(RegistrationValidationException("Email address cannot be empty"))
        }
        if (trimmedInstitution.isEmpty()) {
            return Result.failure(RegistrationValidationException("Institution cannot be empty"))
        }
        if (trimmedPurpose.isEmpty()) {
            return Result.failure(RegistrationValidationException("Purpose of visit cannot be empty"))
        }
        if (durationDays < 1) {
            return Result.failure(RegistrationValidationException("Duration must be at least 1 day"))
        }

        val request = CreateRegistrationRequest(
            eventId = trimmedEventId,
            fullName = trimmedFullName,
            email = trimmedEmail,
            institution = trimmedInstitution,
            purpose = trimmedPurpose,
            durationDays = durationDays,
            vehicleNumber = trimmedVehicle,
            answers = answers
        )

        return try {
            val response = apiService.createRegistration(request)
            if (response.isSuccessful && response.body()?.success == true) {
                val data = response.body()?.data
                    ?: return Result.failure(IllegalStateException("Empty registration response body"))
                Result.success(data)
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string())
                    ?: response.body()?.message
                when (response.code()) {
                    400 -> Result.failure(RegistrationValidationException(errorMsg ?: "Invalid registration data"))
                    401 -> Result.failure(RegistrationAuthException(errorMsg ?: "Authentication required. Please sign in."))
                    404 -> Result.failure(RegistrationNotFoundException(errorMsg ?: "Event not found"))
                    409 -> Result.failure(DuplicateRegistrationException(errorMsg ?: "You are already registered for this event"))
                    else -> Result.failure(Exception(errorMsg ?: "Failed to submit registration (${response.code()})"))
                }
            }
        } catch (e: Exception) {
            Result.failure(resolveNetworkException(e))
        }
    }

    override suspend fun getMyRegistrations(): Result<List<RegistrationDto>> {
        return try {
            val response = apiService.getMyRegistrations()
            if (response.isSuccessful && response.body()?.success == true) {
                val data = response.body()?.data
                Result.success(data?.registrations ?: emptyList())
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string())
                    ?: response.body()?.message
                when (response.code()) {
                    401 -> Result.failure(RegistrationAuthException(errorMsg ?: "Authentication required. Please sign in."))
                    else -> Result.failure(Exception(errorMsg ?: "Failed to retrieve registrations (${response.code()})"))
                }
            }
        } catch (e: Exception) {
            Result.failure(resolveNetworkException(e))
        }
    }

    override suspend fun getRegistrationById(registrationId: String): Result<RegistrationDto> {
        val trimmedId = registrationId.trim()
        if (trimmedId.isEmpty()) {
            return Result.failure(RegistrationValidationException("Registration ID cannot be empty"))
        }

        return try {
            val response = apiService.getRegistrationById(trimmedId)
            if (response.isSuccessful && response.body()?.success == true) {
                val data = response.body()?.data
                if (data?.registration != null) {
                    Result.success(data.registration)
                } else {
                    Result.failure(RegistrationNotFoundException("Registration record not found"))
                }
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string())
                    ?: response.body()?.message
                when (response.code()) {
                    401 -> Result.failure(RegistrationAuthException(errorMsg ?: "Authentication required. Please sign in."))
                    404 -> Result.failure(RegistrationNotFoundException(errorMsg ?: "Registration not found"))
                    else -> Result.failure(Exception(errorMsg ?: "Failed to retrieve registration details (${response.code()})"))
                }
            }
        } catch (e: Exception) {
            // Resilient fallback: search getMyRegistrations() cache/network
            val fallback = getMyRegistrations()
            if (fallback.isSuccess) {
                val match = fallback.getOrNull()?.find { it.id == trimmedId || it.eventId == trimmedId || it.event?.slug == trimmedId }
                if (match != null) {
                    return Result.success(match)
                }
            }
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
