package com.mpc.propass.data.repository

import com.mpc.propass.network.NetworkClient
import com.mpc.propass.network.api.ProPassApiService
import com.mpc.propass.network.model.ApiError
import com.mpc.propass.network.model.MyPassResponseData
import java.io.IOException

/**
 * Exceptions representing specific Digital Pass retrieval failure scenarios.
 */
class NoActivePassException(message: String = "No digital pass found for this user account") : Exception(message)
class PassAuthException(message: String = "Authentication required. Please sign in.") : Exception(message)

/**
 * Repository interface for Digital Pass operations.
 */
interface DigitalPassRepository {
    suspend fun getMyDigitalPass(): Result<MyPassResponseData>
}

/**
 * Default implementation of [DigitalPassRepository] interacting with [ProPassApiService].
 */
class DigitalPassRepositoryImpl(
    private val apiService: ProPassApiService = NetworkClient.apiService
) : DigitalPassRepository {

    private val errorAdapter = NetworkClient.moshi.adapter(ApiError::class.java)

    override suspend fun getMyDigitalPass(): Result<MyPassResponseData> {
        return try {
            val response = apiService.getMyDigitalPass()
            if (response.isSuccessful && response.body()?.success == true) {
                val data = response.body()?.data
                    ?: return Result.failure(IllegalStateException("Empty pass response body"))
                Result.success(data)
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string())
                    ?: response.body()?.message
                when (response.code()) {
                    401 -> Result.failure(PassAuthException(errorMsg ?: "Authentication required. Please sign in."))
                    404 -> Result.failure(NoActivePassException(errorMsg ?: "No digital pass found for this user account"))
                    else -> Result.failure(Exception(errorMsg ?: "Failed to retrieve digital pass (${response.code()})"))
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
