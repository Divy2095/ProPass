package com.mpc.propass.network.interceptor

import okhttp3.Interceptor
import okhttp3.Response

/**
 * OkHttp Interceptor responsible for:
 * 1. Attaching `Accept: application/json` to all outgoing requests.
 * 2. Attaching `Authorization: Bearer <token>` when an access token is available from [TokenProvider].
 * 3. Preserving any existing Authorization header explicitly set on the request.
 */
class AuthInterceptor(
    private val tokenProvider: TokenProvider = NoOpTokenProvider
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val requestBuilder = originalRequest.newBuilder()
            .header("Accept", "application/json")

        // Only inject Authorization if not already explicitly provided on the request
        if (originalRequest.header("Authorization") == null) {
            val token = tokenProvider.getAccessToken()
            if (!token.isNullOrBlank()) {
                requestBuilder.header("Authorization", "Bearer $token")
            }
        }

        return chain.proceed(requestBuilder.build())
    }
}
