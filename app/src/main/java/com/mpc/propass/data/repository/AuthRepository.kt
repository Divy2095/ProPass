package com.mpc.propass.data.repository

import com.mpc.propass.data.local.TokenStorage
import com.mpc.propass.network.NetworkClient
import com.mpc.propass.network.api.ProPassApiService
import com.mpc.propass.network.model.ApiError
import com.mpc.propass.network.model.AuthResponseData
import com.mpc.propass.network.model.AuthTokensDto
import com.mpc.propass.network.model.AuthUserDto
import com.mpc.propass.network.model.LoginRequest
import com.mpc.propass.network.model.RefreshTokenRequest
import com.mpc.propass.network.model.RegisterRequest
import kotlinx.coroutines.flow.Flow
import java.io.IOException

/**
 * Authentication repository interface defining operations for login, registration,
 * token refreshment, logout, and session lifecycle management.
 */
interface AuthRepository {
    suspend fun login(email: String, password: String): Result<AuthResponseData>
    suspend fun register(email: String, password: String): Result<AuthResponseData>
    suspend fun refreshToken(): Result<AuthTokensDto>
    suspend fun logout(): Result<Unit>
    suspend fun checkSession(): Result<AuthUserDto>

    fun hasActiveSession(): Boolean
    fun getAccessToken(): String?
    fun getUserId(): String?
    fun getUserEmail(): String?
    fun getUserRole(): String?
    fun isOrganizer(): Boolean
    val userRoleFlow: Flow<String?>
    val isAuthenticatedFlow: Flow<Boolean>
}

/**
 * Default implementation of [AuthRepository] integrating [ProPassApiService] with [TokenStorage].
 */
class AuthRepositoryImpl(
    private val apiService: ProPassApiService = NetworkClient.apiService,
    private val tokenStorage: TokenStorage
) : AuthRepository {

    private val errorAdapter = NetworkClient.moshi.adapter(ApiError::class.java)

    override suspend fun login(email: String, password: String): Result<AuthResponseData> {
        return try {
            val response = apiService.login(LoginRequest(email.trim().lowercase(), password))
            if (response.isSuccessful && response.body()?.success == true) {
                val data = response.body()?.data
                    ?: return Result.failure(IllegalStateException("Empty response body"))

                tokenStorage.saveTokens(data.tokens.accessToken, data.tokens.refreshToken)
                tokenStorage.saveUser(data.user.id, data.user.email, data.user.role)
                Result.success(data)
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string())
                    ?: response.body()?.message
                    ?: "Authentication failed"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(resolveNetworkException(e))
        }
    }

    override suspend fun register(email: String, password: String): Result<AuthResponseData> {
        return try {
            val response = apiService.register(RegisterRequest(email.trim().lowercase(), password))
            if (response.isSuccessful && response.body()?.success == true) {
                val data = response.body()?.data
                    ?: return Result.failure(IllegalStateException("Empty response body"))

                tokenStorage.saveTokens(data.tokens.accessToken, data.tokens.refreshToken)
                tokenStorage.saveUser(data.user.id, data.user.email, data.user.role)
                Result.success(data)
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string())
                    ?: response.body()?.message
                    ?: "Registration failed"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(resolveNetworkException(e))
        }
    }

    override suspend fun refreshToken(): Result<AuthTokensDto> {
        val currentRefreshToken = tokenStorage.getRefreshToken()
            ?: return Result.failure(IllegalStateException("No refresh token available"))

        return try {
            val response = apiService.refreshToken(RefreshTokenRequest(currentRefreshToken))
            if (response.isSuccessful && response.body()?.success == true) {
                val data = response.body()?.data
                    ?: return Result.failure(IllegalStateException("Empty response body"))

                tokenStorage.saveTokens(data.tokens.accessToken, data.tokens.refreshToken)
                Result.success(data.tokens)
            } else {
                // If refresh token is expired or rejected (e.g. 401), clear local session
                if (response.code() == 401) {
                    tokenStorage.clear()
                }
                val errorMsg = parseErrorMessage(response.errorBody()?.string())
                    ?: "Failed to refresh token"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(resolveNetworkException(e))
        }
    }

    override suspend fun logout(): Result<Unit> {
        val currentRefreshToken = tokenStorage.getRefreshToken()

        if (!currentRefreshToken.isNullOrBlank()) {
            try {
                apiService.logout(RefreshTokenRequest(currentRefreshToken))
            } catch (_: Exception) {
                // Gracefully ignore network failures during logout to guarantee local wipe
            }
        }

        // Always clear tokens locally
        tokenStorage.clear()
        return Result.success(Unit)
    }

    override suspend fun checkSession(): Result<AuthUserDto> {
        if (!hasActiveSession()) {
            return Result.failure(IllegalStateException("No active session"))
        }

        return try {
            val response = apiService.getCurrentUser()
            if (response.isSuccessful && response.body()?.success == true) {
                val user = response.body()?.data?.user
                    ?: return Result.failure(IllegalStateException("Empty user data"))
                tokenStorage.saveUser(user.id, user.email, user.role)
                Result.success(user)
            } else if (response.code() == 401) {
                // Access token may have expired; attempt token refresh
                val refreshResult = refreshToken()
                if (refreshResult.isSuccess) {
                    // Retry getCurrentUser once with refreshed token
                    val retryResponse = apiService.getCurrentUser()
                    if (retryResponse.isSuccessful && retryResponse.body()?.success == true) {
                        val user = retryResponse.body()?.data?.user
                            ?: return Result.failure(IllegalStateException("Empty user data"))
                        Result.success(user)
                    } else {
                        tokenStorage.clear()
                        Result.failure(IllegalStateException("Session expired"))
                    }
                } else {
                    tokenStorage.clear()
                    Result.failure(IllegalStateException("Session expired"))
                }
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string())
                    ?: "Session check failed"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(resolveNetworkException(e))
        }
    }

    override fun hasActiveSession(): Boolean {
        return !tokenStorage.getAccessToken().isNullOrBlank() || !tokenStorage.getRefreshToken().isNullOrBlank()
    }

    override fun getAccessToken(): String? = tokenStorage.getAccessToken()

    override fun getUserId(): String? = tokenStorage.getUserId()

    override fun getUserEmail(): String? = tokenStorage.getUserEmail()

    override fun getUserRole(): String? = tokenStorage.getUserRole()

    override fun isOrganizer(): Boolean =
        tokenStorage.getUserRole()?.equals(com.mpc.propass.network.model.UserRole.ORGANIZER.name, ignoreCase = true) == true

    override val userRoleFlow: Flow<String?> = tokenStorage.userRoleFlow

    override val isAuthenticatedFlow: Flow<Boolean> = tokenStorage.isAuthenticatedFlow

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
