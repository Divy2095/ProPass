package com.mpc.propass.network.api

import com.mpc.propass.network.model.ApiResponse
import com.squareup.moshi.Json
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

/**
 * Foundational Retrofit API service interface for the ProPass Digital Identity System.
 *
 * Establishes the contract for the Fastify backend endpoints implemented in Phases 1 & 2:
 * - Health check
 * - Authentication (Register, Login, Refresh, Logout, Me)
 * - User Profile & Home Dashboard
 * - Events & QR Code validation
 * - Event Registrations
 * - Digital Pass
 */
interface ProPassApiService {

    // ==========================================
    // System & Health
    // ==========================================

    @GET("health")
    suspend fun getHealth(): Response<ApiResponse<Map<String, Any?>>>

    // ==========================================
    // Authentication (Phase 2A)
    // ==========================================

    @POST("api/v1/auth/register")
    suspend fun register(
        @Body request: Map<String, @JvmSuppressWildcards Any>
    ): Response<ApiResponse<Map<String, Any?>>>

    @POST("api/v1/auth/login")
    suspend fun login(
        @Body request: Map<String, @JvmSuppressWildcards Any>
    ): Response<ApiResponse<Map<String, Any?>>>

    @POST("api/v1/auth/refresh")
    suspend fun refreshToken(
        @Body request: Map<String, @JvmSuppressWildcards Any>
    ): Response<ApiResponse<Map<String, Any?>>>

    @POST("api/v1/auth/logout")
    suspend fun logout(
        @Body request: Map<String, @JvmSuppressWildcards Any> = emptyMap()
    ): Response<ApiResponse<Map<String, Any?>>>

    @GET("api/v1/auth/me")
    suspend fun getCurrentUser(): Response<ApiResponse<Map<String, Any?>>>

    // ==========================================
    // User Profile & Dashboard (Phase 2B)
    // ==========================================

    @GET("api/v1/users/profile")
    suspend fun getUserProfile(): Response<ApiResponse<Map<String, Any?>>>

    @PUT("api/v1/users/profile")
    suspend fun updateUserProfile(
        @Body request: Map<String, @JvmSuppressWildcards Any?>
    ): Response<ApiResponse<Map<String, Any?>>>

    @GET("api/v1/dashboard")
    suspend fun getDashboard(): Response<ApiResponse<Map<String, Any?>>>

    // ==========================================
    // Events & QR Validation (Phase 2C)
    // ==========================================

    @GET("api/v1/events/{eventId}")
    suspend fun getEvent(
        @Path("eventId") eventId: String
    ): Response<ApiResponse<Map<String, Any?>>>

    @POST("api/v1/events/validate-qr")
    suspend fun validateQr(
        @Body request: Map<String, @JvmSuppressWildcards String>
    ): Response<ApiResponse<Map<String, Any?>>>

    // ==========================================
    // Event Registrations (Phase 2D)
    // ==========================================

    @POST("api/v1/registrations")
    suspend fun createRegistration(
        @Body request: Map<String, @JvmSuppressWildcards Any?>
    ): Response<ApiResponse<Map<String, Any?>>>

    @GET("api/v1/registrations/my")
    suspend fun getMyRegistrations(): Response<ApiResponse<Map<String, Any?>>>

    // ==========================================
    // Digital Pass (Phase 2E)
    // ==========================================

    @GET("api/v1/passes/me")
    suspend fun getMyDigitalPass(): Response<ApiResponse<Map<String, Any?>>>
}
