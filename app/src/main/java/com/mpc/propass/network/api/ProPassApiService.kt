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
        @Body request: com.mpc.propass.network.model.RegisterRequest
    ): Response<ApiResponse<com.mpc.propass.network.model.AuthResponseData>>

    @POST("api/v1/auth/login")
    suspend fun login(
        @Body request: com.mpc.propass.network.model.LoginRequest
    ): Response<ApiResponse<com.mpc.propass.network.model.AuthResponseData>>

    @POST("api/v1/auth/refresh")
    suspend fun refreshToken(
        @Body request: com.mpc.propass.network.model.RefreshTokenRequest
    ): Response<ApiResponse<com.mpc.propass.network.model.RefreshResponseData>>

    @POST("api/v1/auth/logout")
    suspend fun logout(
        @Body request: com.mpc.propass.network.model.RefreshTokenRequest
    ): Response<ApiResponse<Map<String, Any?>>>

    @GET("api/v1/auth/me")
    suspend fun getCurrentUser(): Response<ApiResponse<com.mpc.propass.network.model.MeResponseData>>

    // ==========================================
    // User Profile & Dashboard (Phase 2B)
    // ==========================================

    @GET("api/v1/users/profile")
    suspend fun getUserProfile(): Response<ApiResponse<com.mpc.propass.network.model.UserProfileResponseData>>

    @PUT("api/v1/users/profile")
    suspend fun updateUserProfile(
        @Body request: com.mpc.propass.network.model.UpdateProfileRequest
    ): Response<ApiResponse<com.mpc.propass.network.model.UserProfileResponseData>>

    @GET("api/v1/dashboard")
    suspend fun getDashboard(): Response<ApiResponse<com.mpc.propass.network.model.DashboardResponseData>>

    // ==========================================
    // Events & QR Validation (Phase 2C)
    // ==========================================

    @GET("api/v1/events/{eventId}")
    suspend fun getEvent(
        @Path("eventId") eventId: String
    ): Response<ApiResponse<com.mpc.propass.network.model.EventResponseData>>

    @POST("api/v1/events/validate-qr")
    suspend fun validateQr(
        @Body request: com.mpc.propass.network.model.ValidateQrRequest
    ): Response<ApiResponse<com.mpc.propass.network.model.ValidateQrResponseData>>

    // ==========================================
    // Event Registrations (Phase 2D)
    // ==========================================

    @POST("api/v1/registrations")
    suspend fun createRegistration(
        @Body request: com.mpc.propass.network.model.CreateRegistrationRequest
    ): Response<ApiResponse<com.mpc.propass.network.model.CreateRegistrationResponseData>>

    @GET("api/v1/registrations/my")
    suspend fun getMyRegistrations(): Response<ApiResponse<com.mpc.propass.network.model.MyRegistrationsResponseData>>

    @GET("api/v1/registrations/{registrationId}")
    suspend fun getRegistrationById(
        @retrofit2.http.Path("registrationId") registrationId: String
    ): Response<ApiResponse<com.mpc.propass.network.model.RegistrationDetailResponseData>>

    // ==========================================
    // Digital Pass (Phase 2E)
    // ==========================================

    @GET("api/v1/passes/me")
    suspend fun getMyDigitalPass(): Response<ApiResponse<com.mpc.propass.network.model.MyPassResponseData>>

    // ==========================================
    // Organizer Events (Phase 4B)
    // ==========================================

    @POST("api/v1/events")
    suspend fun createEvent(
        @Body request: com.mpc.propass.network.model.CreateEventRequest
    ): Response<ApiResponse<com.mpc.propass.network.model.CreateEventResponseData>>

    @GET("api/v1/organizer/events")
    suspend fun getOrganizerEvents(): Response<ApiResponse<com.mpc.propass.network.model.OrganizerEventsResponseData>>

    // ==========================================
    // Registration Form Persistence (Phase 4C)
    // ==========================================

    @GET("api/v1/events/{eventId}/form")
    suspend fun getEventForm(
        @retrofit2.http.Path("eventId") eventId: String
    ): Response<ApiResponse<com.mpc.propass.network.model.SaveFormResponseData>>

    @retrofit2.http.PUT("api/v1/events/{eventId}/form")
    suspend fun saveEventForm(
        @retrofit2.http.Path("eventId") eventId: String,
        @Body request: com.mpc.propass.network.model.SaveFormRequest
    ): Response<ApiResponse<com.mpc.propass.network.model.SaveFormResponseData>>

    // ==========================================
    // Organizer Registrations (Phase 4D)
    // ==========================================

    @GET("api/v1/organizer/events/{eventId}/registrations")
    suspend fun getEventRegistrations(
        @Path("eventId") eventId: String
    ): Response<ApiResponse<com.mpc.propass.organizer.model.OrganizerRegistrationsResponseData>>

    @GET("api/v1/organizer/registrations/{registrationId}")
    suspend fun getRegistrationDetails(
        @Path("registrationId") registrationId: String
    ): Response<ApiResponse<com.mpc.propass.organizer.model.OrganizerRegistrationDetailResponseData>>
}
