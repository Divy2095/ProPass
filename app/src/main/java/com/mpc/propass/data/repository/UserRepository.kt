package com.mpc.propass.data.repository

import com.mpc.propass.network.NetworkClient
import com.mpc.propass.network.api.ProPassApiService
import com.mpc.propass.network.model.ApiError
import com.mpc.propass.network.model.UpdateProfileRequest
import com.mpc.propass.network.model.UserProfileResponseData
import java.io.IOException

/**
 * Repository interface for User & Profile data operations.
 */
interface UserRepository {
    suspend fun getUserProfile(): Result<UserProfileResponseData>
    suspend fun updateUserProfile(request: UpdateProfileRequest): Result<UserProfileResponseData>
}

/**
 * Default implementation of [UserRepository] interacting with [ProPassApiService].
 */
class UserRepositoryImpl(
    private val apiService: ProPassApiService = NetworkClient.apiService
) : UserRepository {

    private val errorAdapter = NetworkClient.moshi.adapter(ApiError::class.java)

    override suspend fun getUserProfile(): Result<UserProfileResponseData> {
        return try {
            val response = apiService.getUserProfile()
            if (response.isSuccessful && response.body()?.success == true) {
                val data = response.body()?.data
                    ?: return Result.failure(IllegalStateException("Empty profile response body"))
                Result.success(data)
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string())
                    ?: response.body()?.message
                    ?: "Failed to load profile"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(resolveNetworkException(e))
        }
    }

    override suspend fun updateUserProfile(request: UpdateProfileRequest): Result<UserProfileResponseData> {
        return try {
            val response = apiService.updateUserProfile(request)
            if (response.isSuccessful && response.body()?.success == true) {
                val data = response.body()?.data
                    ?: return Result.failure(IllegalStateException("Empty profile update response body"))
                Result.success(data)
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string())
                    ?: response.body()?.message
                    ?: "Failed to update profile"
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
