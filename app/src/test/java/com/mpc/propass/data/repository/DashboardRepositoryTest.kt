package com.mpc.propass.data.repository

import com.mpc.propass.network.NetworkClient
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

class DashboardRepositoryTest {

    private lateinit var server: MockWebServer
    private lateinit var dashboardRepository: DashboardRepository

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()

        val apiService = NetworkClient.createApiService(
            baseUrl = server.url("/").toString(),
            enableLogging = false
        )

        dashboardRepository = DashboardRepositoryImpl(apiService = apiService)
    }

    @After
    fun tearDown() {
        server.shutdown()
        NetworkClient.reset()
    }

    @Test
    fun getDashboard_success() = runBlocking {
        val json = """
            {
                "success": true,
                "data": {
                    "greeting": "Hello, Sarah",
                    "user": {
                        "id": "u-1",
                        "email": "sarah.jenkins@example.com",
                        "role": "USER"
                    },
                    "profile": {
                        "id": "p-1",
                        "fullName": "Sarah Jenkins",
                        "title": "Senior UX Researcher",
                        "organization": "TechFlow Inc.",
                        "phone": "+1 (555) 018-9234",
                        "linkedinUrl": "https://linkedin.com/in/sarahjenkins",
                        "avatarUrl": "https://propass.id/avatars/sarah.jpg",
                        "isVerified": true,
                        "completionScore": 85
                    },
                    "pass": {
                        "id": "pass-1",
                        "passNumber": "PP-2024-0042",
                        "tier": "PREMIUM",
                        "isActive": true,
                        "qrPayload": "propass:pass:PP-2024-0042",
                        "expiresAt": "2026-12-31T23:59:59Z"
                    },
                    "recentActivity": [
                        {
                            "registrationId": "reg-1",
                            "eventId": "e-1",
                            "eventSlug": "techconf-2024",
                            "eventTitle": "TechConf 2024",
                            "eventOverline": "Annual Conference",
                            "eventSubtitle": "Main Auditorium",
                            "eventLocation": "San Francisco, CA",
                            "eventStartDate": "2024-11-15T09:00:00Z",
                            "purpose": "GENERAL_ATTENDEE",
                            "durationDays": 3,
                            "status": "CONFIRMED",
                            "registeredAt": "2026-09-01T10:00:00Z"
                        }
                    ]
                },
                "timestamp": "2026-09-04T12:00:00Z"
            }
        """.trimIndent()

        server.enqueue(MockResponse().setResponseCode(200).setBody(json))

        val result = dashboardRepository.getDashboard()
        assertTrue(result.isSuccess)

        val data = result.getOrNull()
        assertNotNull(data)
        assertEquals("Hello, Sarah", data!!.greeting)
        assertEquals("sarah.jenkins@example.com", data.user.email)
        assertEquals("Sarah Jenkins", data.profile?.fullName)
        assertEquals("PP-2024-0042", data.pass?.passNumber)
        assertEquals(1, data.recentActivity.size)
        assertEquals("TechConf 2024", data.recentActivity[0].eventTitle)

        val recordedRequest = server.takeRequest()
        assertEquals("/api/v1/dashboard", recordedRequest.path)
        assertEquals("GET", recordedRequest.method)
    }

    @Test
    fun getDashboard_emptyActivity_success() = runBlocking {
        val json = """
            {
                "success": true,
                "data": {
                    "greeting": "Hello, Alex",
                    "user": {
                        "id": "u-2",
                        "email": "alex@example.com",
                        "role": "USER"
                    },
                    "profile": null,
                    "pass": null,
                    "recentActivity": []
                },
                "timestamp": "2026-09-04T12:00:00Z"
            }
        """.trimIndent()

        server.enqueue(MockResponse().setResponseCode(200).setBody(json))

        val result = dashboardRepository.getDashboard()
        assertTrue(result.isSuccess)

        val data = result.getOrNull()
        assertNotNull(data)
        assertEquals("Hello, Alex", data!!.greeting)
        assertTrue(data.recentActivity.isEmpty())
    }

    @Test
    fun getDashboard_unauthorized_401() = runBlocking {
        val json = """
            {
                "success": false,
                "error": "Unauthorized",
                "message": "Session expired or invalid token",
                "statusCode": 401,
                "timestamp": "2026-09-04T12:00:00Z"
            }
        """.trimIndent()

        server.enqueue(MockResponse().setResponseCode(401).setBody(json))

        val result = dashboardRepository.getDashboard()
        assertFalse(result.isSuccess)
        assertEquals("Session expired or invalid token", result.exceptionOrNull()?.message)
    }

    @Test
    fun getDashboard_networkError() = runBlocking {
        server.shutdown()

        val result = dashboardRepository.getDashboard()
        assertFalse(result.isSuccess)
        assertTrue(result.exceptionOrNull() is IOException)
    }

    @Test
    fun getDashboard_serverError_500() = runBlocking {
        val json = """
            {
                "success": false,
                "error": "Internal Server Error",
                "message": "Database query failed",
                "statusCode": 500,
                "timestamp": "2026-09-04T12:00:00Z"
            }
        """.trimIndent()

        server.enqueue(MockResponse().setResponseCode(500).setBody(json))

        val result = dashboardRepository.getDashboard()
        assertFalse(result.isSuccess)
        assertEquals("Database query failed", result.exceptionOrNull()?.message)
    }
}
