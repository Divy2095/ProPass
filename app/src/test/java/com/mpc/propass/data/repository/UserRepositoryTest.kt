package com.mpc.propass.data.repository

import com.mpc.propass.network.NetworkClient
import com.mpc.propass.network.model.UpdateProfileRequest
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

class UserRepositoryTest {

    private lateinit var server: MockWebServer
    private lateinit var userRepository: UserRepository

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()

        val apiService = NetworkClient.createApiService(
            baseUrl = server.url("/").toString(),
            enableLogging = false
        )

        userRepository = UserRepositoryImpl(apiService = apiService)
    }

    @After
    fun tearDown() {
        server.shutdown()
        NetworkClient.reset()
    }

    @Test
    fun getUserProfile_success() = runBlocking {
        val json = """
            {
                "success": true,
                "data": {
                    "user": {
                        "id": "u-1",
                        "email": "sarah.jenkins@example.com",
                        "role": "USER"
                    },
                    "profile": {
                        "id": "p-1",
                        "userId": "u-1",
                        "fullName": "Sarah Jenkins",
                        "title": "Senior UX Researcher",
                        "organization": "TechFlow Inc.",
                        "phone": "+1 (555) 018-9234",
                        "linkedinUrl": "https://linkedin.com/in/sarahjenkins",
                        "avatarUrl": "https://propass.id/avatars/sarah.jpg",
                        "isVerified": true,
                        "completionScore": 90
                    }
                },
                "timestamp": "2026-09-04T12:00:00Z"
            }
        """.trimIndent()

        server.enqueue(MockResponse().setResponseCode(200).setBody(json))

        val result = userRepository.getUserProfile()
        assertTrue(result.isSuccess)

        val data = result.getOrNull()
        assertNotNull(data)
        assertEquals("sarah.jenkins@example.com", data!!.user.email)
        assertEquals("Sarah Jenkins", data.profile?.fullName)
        assertEquals(90, data.profile?.completionScore)

        val recordedRequest = server.takeRequest()
        assertEquals("/api/v1/users/profile", recordedRequest.path)
        assertEquals("GET", recordedRequest.method)
    }

    @Test
    fun getUserProfile_unauthorized_401() = runBlocking {
        val json = """
            {
                "success": false,
                "error": "Unauthorized",
                "message": "Missing or invalid authorization token",
                "statusCode": 401,
                "timestamp": "2026-09-04T12:00:00Z"
            }
        """.trimIndent()

        server.enqueue(MockResponse().setResponseCode(401).setBody(json))

        val result = userRepository.getUserProfile()
        assertFalse(result.isSuccess)
        assertEquals("Missing or invalid authorization token", result.exceptionOrNull()?.message)
    }

    @Test
    fun getUserProfile_notFound_404() = runBlocking {
        val json = """
            {
                "success": false,
                "error": "Not Found",
                "message": "User account not found",
                "statusCode": 404,
                "timestamp": "2026-09-04T12:00:00Z"
            }
        """.trimIndent()

        server.enqueue(MockResponse().setResponseCode(404).setBody(json))

        val result = userRepository.getUserProfile()
        assertFalse(result.isSuccess)
        assertEquals("User account not found", result.exceptionOrNull()?.message)
    }

    @Test
    fun getUserProfile_networkError() = runBlocking {
        server.shutdown()

        val result = userRepository.getUserProfile()
        assertFalse(result.isSuccess)
        assertTrue(result.exceptionOrNull() is IOException)
    }

    @Test
    fun updateUserProfile_success() = runBlocking {
        val responseJson = """
            {
                "success": true,
                "message": "Profile updated successfully",
                "data": {
                    "user": {
                        "id": "u-1",
                        "email": "sarah.jenkins@example.com",
                        "role": "USER"
                    },
                    "profile": {
                        "id": "p-1",
                        "userId": "u-1",
                        "fullName": "Sarah Jenkins",
                        "title": "Lead Product Designer",
                        "organization": "Acme Corp",
                        "phone": "+1 (555) 999-8888",
                        "completionScore": 95
                    }
                },
                "timestamp": "2026-09-04T12:00:00Z"
            }
        """.trimIndent()

        server.enqueue(MockResponse().setResponseCode(200).setBody(responseJson))

        val request = UpdateProfileRequest(
            title = "Lead Product Designer",
            organization = "Acme Corp",
            phone = "+1 (555) 999-8888"
        )

        val result = userRepository.updateUserProfile(request)
        assertTrue(result.isSuccess)

        val data = result.getOrNull()
        assertNotNull(data)
        assertEquals("Lead Product Designer", data!!.profile?.title)
        assertEquals("Acme Corp", data.profile?.organization)
        assertEquals(95, data.profile?.completionScore)

        val recordedRequest = server.takeRequest()
        assertEquals("/api/v1/users/profile", recordedRequest.path)
        assertEquals("PUT", recordedRequest.method)
        val body = recordedRequest.body.readUtf8()
        assertTrue(body.contains("\"title\":\"Lead Product Designer\""))
        assertTrue(body.contains("\"organization\":\"Acme Corp\""))
    }

    @Test
    fun updateUserProfile_validationError_400() = runBlocking {
        val json = """
            {
                "success": false,
                "error": "Validation Error",
                "message": "LinkedIn URL must be a valid URL",
                "details": [
                    { "field": "linkedinUrl", "message": "LinkedIn URL must be a valid URL" }
                ],
                "statusCode": 400,
                "timestamp": "2026-09-04T12:00:00Z"
            }
        """.trimIndent()

        server.enqueue(MockResponse().setResponseCode(400).setBody(json))

        val request = UpdateProfileRequest(linkedinUrl = "invalid-url")
        val result = userRepository.updateUserProfile(request)

        assertFalse(result.isSuccess)
        assertEquals("LinkedIn URL must be a valid URL", result.exceptionOrNull()?.message)
    }

    @Test
    fun updateUserProfile_serverError_500() = runBlocking {
        val json = """
            {
                "success": false,
                "error": "Internal Server Error",
                "message": "Database write error",
                "statusCode": 500,
                "timestamp": "2026-09-04T12:00:00Z"
            }
        """.trimIndent()

        server.enqueue(MockResponse().setResponseCode(500).setBody(json))

        val request = UpdateProfileRequest(title = "New Title")
        val result = userRepository.updateUserProfile(request)

        assertFalse(result.isSuccess)
        assertEquals("Database write error", result.exceptionOrNull()?.message)
    }
}
