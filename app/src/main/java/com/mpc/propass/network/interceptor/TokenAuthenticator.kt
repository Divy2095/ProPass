package com.mpc.propass.network.interceptor

import com.mpc.propass.data.local.TokenStorage
import com.mpc.propass.network.NetworkClient
import com.mpc.propass.network.model.ApiResponse
import com.mpc.propass.network.model.RefreshResponseData
import com.mpc.propass.network.model.RefreshTokenRequest
import com.squareup.moshi.Types
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * OkHttp [Authenticator] that transparently intercepts HTTP 401 Unauthorized responses,
 * acquires a refreshed access token using the persisted 7-day refresh token,
 * updates [TokenStorage], and automatically retries the failed request.
 */
class TokenAuthenticator(
    private val tokenStorage: TokenStorage
) : Authenticator {

    private val refreshClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val refreshResponseType = Types.newParameterizedType(
        ApiResponse::class.java,
        RefreshResponseData::class.java
    )
    private val refreshResponseAdapter = NetworkClient.moshi.adapter<ApiResponse<RefreshResponseData>>(refreshResponseType)
    private val refreshRequestAdapter = NetworkClient.moshi.adapter(RefreshTokenRequest::class.java)

    override fun authenticate(route: Route?, response: Response): Request? {
        val path = response.request.url.encodedPath
        // Do not attempt token refresh for authentication endpoints to prevent infinite loops
        if (path.contains("/auth/refresh") || path.contains("/auth/login") || path.contains("/auth/register")) {
            return null
        }

        // Limit retry attempts on the same chain
        if (responseCount(response) >= 3) {
            return null
        }

        synchronized(this) {
            val currentAccessToken = tokenStorage.getAccessToken()
            val requestToken = response.request.header("Authorization")?.removePrefix("Bearer ")?.trim()

            // If another concurrent request already refreshed the access token, retry immediately
            if (!currentAccessToken.isNullOrBlank() && currentAccessToken != requestToken) {
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $currentAccessToken")
                    .build()
            }

            val refreshToken = tokenStorage.getRefreshToken() ?: return null

            val requestBodyJson = refreshRequestAdapter.toJson(RefreshTokenRequest(refreshToken))
            val refreshUrl = response.request.url.newBuilder()
                .encodedPath("/api/v1/auth/refresh")
                .query(null)
                .build()

            val refreshRequest = Request.Builder()
                .url(refreshUrl)
                .post(requestBodyJson.toRequestBody("application/json; charset=utf-8".toMediaType()))
                .header("Accept", "application/json")
                .build()

            try {
                val refreshResponse = refreshClient.newCall(refreshRequest).execute()
                if (refreshResponse.isSuccessful) {
                    val bodyString = refreshResponse.body?.string()
                    val parsed = if (!bodyString.isNullOrBlank()) refreshResponseAdapter.fromJson(bodyString) else null
                    val newTokens = parsed?.data?.tokens
                    if (newTokens != null && newTokens.accessToken.isNotBlank()) {
                        runBlocking(Dispatchers.IO) {
                            tokenStorage.saveTokens(newTokens.accessToken, newTokens.refreshToken)
                        }
                        return response.request.newBuilder()
                            .header("Authorization", "Bearer ${newTokens.accessToken}")
                            .build()
                    }
                } else if (refreshResponse.code == 401) {
                    // Refresh token is expired or revoked; invalidate local session
                    runBlocking(Dispatchers.IO) {
                        tokenStorage.clear()
                    }
                    return null
                }
            } catch (_: IOException) {
                return null
            }
        }

        return null
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}
