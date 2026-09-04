package com.mpc.propass.data.repository

import com.mpc.propass.network.NetworkClient
import com.mpc.propass.network.api.ProPassApiService
import com.mpc.propass.network.model.ApiError
import com.mpc.propass.network.model.DashboardResponseData
import java.io.IOException

/**
 * Repository interface for Home Dashboard aggregated data operations.
 */
interface DashboardRepository {
    suspend fun getDashboard(): Result<DashboardResponseData>
}

/**
 * Default implementation of [DashboardRepository] interacting with [ProPassApiService].
 */
class DashboardRepositoryImpl(
    private val apiService: ProPassApiService = NetworkClient.apiService
) : DashboardRepository {

    private val errorAdapter = NetworkClient.moshi.adapter(ApiError::class.java)

    override suspend fun getDashboard(): Result<DashboardResponseData> {
        return try {
            val response = apiService.getDashboard()
            if (response.isSuccessful && response.body()?.success == true) {
                val data = response.body()?.data
                    ?: return Result.failure(IllegalStateException("Empty dashboard response body"))
                Result.success(data)
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string())
                    ?: response.body()?.message
                    ?: "Failed to load dashboard"
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
