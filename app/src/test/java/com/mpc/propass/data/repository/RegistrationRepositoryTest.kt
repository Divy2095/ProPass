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

class RegistrationRepositoryTest {

    private lateinit var server: MockWebServer
    private lateinit var registrationRepository: RegistrationRepository

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()

        val apiService = NetworkClient.createApiService(
            baseUrl = server.url("/").toString(),
            enableLogging = false
        )

        registrationRepository = RegistrationRepositoryImpl(apiService = apiService)
    }

    @After
    fun tearDown() {
        server.shutdown()
        NetworkClient.reset()
    }

    @Test
    fun createRegistration_success_201() = runBlocking {
        val json = """
            {
                "success": true,
                "message": "Registration confirmed successfully",
                "data": {
                    "registration": {
                        "id": "reg-101",
                        "userId": "usr-101",
                        "eventId": "evt-101",
                        "fullName": "Sarah Jenkins",
                        "email": "sarah.jenkins@example.com",
                        "institution": "TechFlow Inc.",
                        "purpose": "GENERAL_ATTENDEE",
                        "durationDays": 3,
                        "vehicleNumber": "CA-9876-TX",
                        "status": "CONFIRMED",
                        "registeredAt": "2026-09-04T12:00:00Z",
                        "event": {
                            "id": "evt-101",
                            "slug": "techconf-2024",
                            "title": "TechConf 2024"
                        }
                    }
                },
                "timestamp": "2026-09-04T12:00:00Z"
            }
        """.trimIndent()

        server.enqueue(MockResponse().setResponseCode(201).setBody(json))

        val result = registrationRepository.createRegistration(
            eventId = "techconf-2024",
            fullName = "Sarah Jenkins",
            email = "sarah.jenkins@example.com",
            institution = "TechFlow Inc.",
            purpose = "GENERAL_ATTENDEE",
            durationDays = 3,
            vehicleNumber = "CA-9876-TX"
        )

        assertTrue(result.isSuccess)
        val data = result.getOrNull()
        assertNotNull(data)
        assertEquals("reg-101", data!!.registration.id)
        assertEquals("TechConf 2024", data.registration.event?.title)
        assertEquals("CONFIRMED", data.registration.status)

        val recordedRequest = server.takeRequest()
        assertEquals("/api/v1/registrations", recordedRequest.path)
        assertEquals("POST", recordedRequest.method)
        val requestBody = recordedRequest.body.readUtf8()
        assertTrue(requestBody.contains("\"eventId\":\"techconf-2024\""))
        assertTrue(requestBody.contains("\"fullName\":\"Sarah Jenkins\""))
        assertTrue(requestBody.contains("\"durationDays\":3"))
    }

    @Test
    fun createRegistration_duplicate_409() = runBlocking {
        val json = """
            {
                "success": false,
                "error": "Conflict",
                "message": "You are already registered for \"TechConf 2024\"",
                "statusCode": 409,
                "timestamp": "2026-09-04T12:00:00Z"
            }
        """.trimIndent()

        server.enqueue(MockResponse().setResponseCode(409).setBody(json))

        val result = registrationRepository.createRegistration(
            eventId = "techconf-2024",
            fullName = "Sarah Jenkins",
            email = "sarah.jenkins@example.com",
            institution = "TechFlow Inc.",
            purpose = "GENERAL_ATTENDEE",
            durationDays = 3
        )

        assertFalse(result.isSuccess)
        assertTrue(result.exceptionOrNull() is DuplicateRegistrationException)
        assertEquals("You are already registered for \"TechConf 2024\"", result.exceptionOrNull()?.message)
    }

    @Test
    fun createRegistration_validationError_400() = runBlocking {
        val json = """
            {
                "success": false,
                "error": "Validation Error",
                "message": "Duration (6 days) exceeds event maximum allowed duration (3 days)",
                "statusCode": 400,
                "timestamp": "2026-09-04T12:00:00Z"
            }
        """.trimIndent()

        server.enqueue(MockResponse().setResponseCode(400).setBody(json))

        val result = registrationRepository.createRegistration(
            eventId = "techconf-2024",
            fullName = "Sarah Jenkins",
            email = "sarah.jenkins@example.com",
            institution = "TechFlow Inc.",
            purpose = "GENERAL_ATTENDEE",
            durationDays = 6
        )

        assertFalse(result.isSuccess)
        assertTrue(result.exceptionOrNull() is RegistrationValidationException)
        assertEquals("Duration (6 days) exceeds event maximum allowed duration (3 days)", result.exceptionOrNull()?.message)
    }

    @Test
    fun createRegistration_unauthorized_401() = runBlocking {
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

        val result = registrationRepository.createRegistration(
            eventId = "techconf-2024",
            fullName = "Sarah Jenkins",
            email = "sarah.jenkins@example.com",
            institution = "TechFlow Inc.",
            purpose = "GENERAL_ATTENDEE",
            durationDays = 3
        )

        assertFalse(result.isSuccess)
        assertTrue(result.exceptionOrNull() is RegistrationAuthException)
        assertEquals("Session expired or invalid token", result.exceptionOrNull()?.message)
    }

    @Test
    fun createRegistration_emptyEventId() = runBlocking {
        val result = registrationRepository.createRegistration(
            eventId = "  ",
            fullName = "Sarah Jenkins",
            email = "sarah.jenkins@example.com",
            institution = "TechFlow Inc.",
            purpose = "GENERAL_ATTENDEE",
            durationDays = 3
        )

        assertFalse(result.isSuccess)
        assertTrue(result.exceptionOrNull() is RegistrationValidationException)
        assertEquals("Event identifier cannot be empty", result.exceptionOrNull()?.message)
    }

    @Test
    fun createRegistration_invalidDuration() = runBlocking {
        val result = registrationRepository.createRegistration(
            eventId = "techconf-2024",
            fullName = "Sarah Jenkins",
            email = "sarah.jenkins@example.com",
            institution = "TechFlow Inc.",
            purpose = "GENERAL_ATTENDEE",
            durationDays = 0
        )

        assertFalse(result.isSuccess)
        assertTrue(result.exceptionOrNull() is RegistrationValidationException)
        assertEquals("Duration must be at least 1 day", result.exceptionOrNull()?.message)
    }

    @Test
    fun createRegistration_networkError() = runBlocking {
        server.shutdown()

        val result = registrationRepository.createRegistration(
            eventId = "techconf-2024",
            fullName = "Sarah Jenkins",
            email = "sarah.jenkins@example.com",
            institution = "TechFlow Inc.",
            purpose = "GENERAL_ATTENDEE",
            durationDays = 3
        )

        assertFalse(result.isSuccess)
        assertTrue(result.exceptionOrNull() is IOException)
    }

    @Test
    fun getMyRegistrations_success_200() = runBlocking {
        val json = """
            {
                "success": true,
                "data": {
                    "registrations": [
                        {
                            "id": "reg-1",
                            "userId": "usr-1",
                            "eventId": "evt-1",
                            "fullName": "Sarah Jenkins",
                            "email": "sarah.jenkins@example.com",
                            "institution": "TechFlow Inc.",
                            "purpose": "GENERAL_ATTENDEE",
                            "durationDays": 2,
                            "status": "CONFIRMED",
                            "registeredAt": "2026-09-04T12:00:00Z"
                        }
                    ],
                    "count": 1
                },
                "timestamp": "2026-09-04T12:00:00Z"
            }
        """.trimIndent()

        server.enqueue(MockResponse().setResponseCode(200).setBody(json))

        val result = registrationRepository.getMyRegistrations()
        assertTrue(result.isSuccess)
        val list = result.getOrNull()
        assertNotNull(list)
        assertEquals(1, list!!.size)
        assertEquals("reg-1", list[0].id)

        val recordedRequest = server.takeRequest()
        assertEquals("/api/v1/registrations/my", recordedRequest.path)
        assertEquals("GET", recordedRequest.method)
    }

    @Test
    fun getMyRegistrations_unauthorized_401() = runBlocking {
        val json = """
            {
                "success": false,
                "error": "Unauthorized",
                "message": "Token expired",
                "statusCode": 401,
                "timestamp": "2026-09-04T12:00:00Z"
            }
        """.trimIndent()

        server.enqueue(MockResponse().setResponseCode(401).setBody(json))

        val result = registrationRepository.getMyRegistrations()
        assertFalse(result.isSuccess)
        assertTrue(result.exceptionOrNull() is RegistrationAuthException)
    }
}
