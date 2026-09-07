package com.mpc.propass.organizer

import com.mpc.propass.network.NetworkClient
import com.mpc.propass.network.model.EventDto
import com.mpc.propass.organizer.data.OrganizerRegistrationRepository
import com.mpc.propass.organizer.data.OrganizerRegistrationRepositoryImpl
import com.mpc.propass.organizer.model.toOrganizerEvent
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

class OrganizerRegistrationRepositoryTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: OrganizerRegistrationRepository

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()

        val apiService = NetworkClient.createApiService(
            baseUrl = server.url("/").toString(),
            enableLogging = false
        )
        repository = OrganizerRegistrationRepositoryImpl(apiService = apiService)
    }

    @After
    fun tearDown() {
        server.shutdown()
        NetworkClient.reset()
    }

    @Test
    fun getEventRegistrations_success_with_answers_200() = runBlocking {
        val json = """
            {
                "success": true,
                "data": {
                    "event": {
                        "id": "ev-101",
                        "title": "Cloud DevCon 2026",
                        "slug": "cloud-devcon-2026"
                    },
                    "registrations": [
                        {
                            "id": "reg-001",
                            "eventId": "ev-101",
                            "userId": "usr-001",
                            "status": "CONFIRMED",
                            "purpose": "SPEAKER",
                            "durationDays": 2,
                            "vehicleNumber": "CA-TECH-01",
                            "registeredAt": "2026-09-07T10:00:00.000Z",
                            "fullName": "Alice Johnson",
                            "email": "alice@example.com",
                            "institution": "Stanford University",
                            "phone": "+1-555-0101",
                            "attendee": {
                                "id": "usr-001",
                                "fullName": "Alice Johnson",
                                "email": "alice@example.com",
                                "institution": "Stanford University",
                                "phone": "+1-555-0101",
                                "organization": "Stanford AI Lab",
                                "title": "Staff Researcher",
                                "avatarUrl": "https://example.com/avatar1.png"
                            },
                            "answers": [
                                {
                                    "id": "ans-1",
                                    "questionId": "q-tshirt",
                                    "questionLabel": "T-Shirt Size",
                                    "questionType": "MULTIPLE_CHOICE",
                                    "value": "M",
                                    "options": ["S", "M", "L", "XL"],
                                    "orderIndex": 3,
                                    "isDefaultField": false
                                },
                                {
                                    "id": "ans-2",
                                    "questionId": "q-diet",
                                    "questionLabel": "Dietary Restrictions",
                                    "questionType": "CHECKBOX",
                                    "value": "Vegetarian, Gluten-Free",
                                    "options": ["Vegetarian", "Vegan", "Gluten-Free"],
                                    "orderIndex": 4,
                                    "isDefaultField": false
                                }
                            ]
                        }
                    ],
                    "count": 1
                },
                "timestamp": "2026-09-07T12:00:00Z"
            }
        """.trimIndent()

        server.enqueue(MockResponse().setResponseCode(200).setBody(json))

        val result = repository.getEventRegistrations("ev-101")
        assertTrue("Expected success response", result.isSuccess)

        val data = result.getOrNull()
        assertNotNull(data)
        assertEquals(1, data!!.count)
        assertEquals(1, data.registrations.size)
        assertEquals("Cloud DevCon 2026", data.event?.title)

        val reg = data.registrations[0]
        assertEquals("reg-001", reg.id)
        assertEquals("CONFIRMED", reg.status)
        assertEquals("SPEAKER", reg.purpose)
        assertEquals(2, reg.durationDays)
        assertEquals("CA-TECH-01", reg.vehicleNumber)
        assertEquals("Alice Johnson", reg.attendee?.fullName)
        assertEquals("alice@example.com", reg.attendee?.email)
        assertEquals("Stanford AI Lab", reg.attendee?.organization)
        assertEquals("Staff Researcher", reg.attendee?.title)
        assertEquals(2, reg.answers.size)

        val ans1 = reg.answers[0]
        assertEquals("T-Shirt Size", ans1.questionLabel)
        assertEquals("MULTIPLE_CHOICE", ans1.questionType)
        assertEquals("M", ans1.value)

        val ans2 = reg.answers[1]
        assertEquals("Dietary Restrictions", ans2.questionLabel)
        assertEquals("CHECKBOX", ans2.questionType)
        assertEquals("Vegetarian, Gluten-Free", ans2.value)

        val recordedRequest = server.takeRequest()
        assertEquals("GET", recordedRequest.method)
        assertEquals("/api/v1/organizer/events/ev-101/registrations", recordedRequest.path)
    }

    @Test
    fun getEventRegistrations_empty_list_200() = runBlocking {
        val json = """
            {
                "success": true,
                "data": {
                    "event": {
                        "id": "ev-empty",
                        "title": "Empty Event",
                        "slug": "empty-event"
                    },
                    "registrations": [],
                    "count": 0
                },
                "timestamp": "2026-09-07T12:00:00Z"
            }
        """.trimIndent()

        server.enqueue(MockResponse().setResponseCode(200).setBody(json))

        val result = repository.getEventRegistrations("ev-empty")
        assertTrue(result.isSuccess)

        val data = result.getOrNull()
        assertNotNull(data)
        assertEquals(0, data!!.count)
        assertTrue(data.registrations.isEmpty())
    }

    @Test
    fun getEventRegistrations_unauthorized_401() = runBlocking {
        val json = """
            {
                "success": false,
                "error": "Unauthorized",
                "message": "Missing or invalid authorization token",
                "statusCode": 401
            }
        """.trimIndent()

        server.enqueue(MockResponse().setResponseCode(401).setBody(json))

        val result = repository.getEventRegistrations("ev-1")
        assertFalse(result.isSuccess)
        assertTrue(result.exceptionOrNull()?.message?.contains("Missing or invalid authorization token") == true)
    }

    @Test
    fun getEventRegistrations_forbidden_cross_organizer_403() = runBlocking {
        val json = """
            {
                "success": false,
                "error": "Forbidden",
                "message": "You do not have permission to view registrations for this event",
                "statusCode": 403
            }
        """.trimIndent()

        server.enqueue(MockResponse().setResponseCode(403).setBody(json))

        val result = repository.getEventRegistrations("ev-other-organizer")
        assertFalse(result.isSuccess)
        assertTrue(result.exceptionOrNull()?.message?.contains("permission") == true)
    }

    @Test
    fun getEventRegistrations_event_not_found_404() = runBlocking {
        val json = """
            {
                "success": false,
                "error": "Not Found",
                "message": "Event not found",
                "statusCode": 404
            }
        """.trimIndent()

        server.enqueue(MockResponse().setResponseCode(404).setBody(json))

        val result = repository.getEventRegistrations("non-existent-id")
        assertFalse(result.isSuccess)
        assertTrue(result.exceptionOrNull()?.message?.contains("Event not found") == true)
    }

    @Test
    fun getEventRegistrations_server_error_500() = runBlocking {
        val json = """
            {
                "success": false,
                "error": "Internal Server Error",
                "message": "Database connection error",
                "statusCode": 500
            }
        """.trimIndent()

        server.enqueue(MockResponse().setResponseCode(500).setBody(json))

        val result = repository.getEventRegistrations("ev-101")
        assertFalse(result.isSuccess)
        assertTrue(result.exceptionOrNull()?.message?.contains("Database connection error") == true)
    }

    @Test
    fun getRegistrationDetail_success_200() = runBlocking {
        val json = """
            {
                "success": true,
                "data": {
                    "registration": {
                        "id": "reg-999",
                        "eventId": "ev-101",
                        "userId": "usr-888",
                        "status": "CONFIRMED",
                        "purpose": "GENERAL_ATTENDEE",
                        "durationDays": 1,
                        "vehicleNumber": "NY-PRO-99",
                        "registeredAt": "2026-09-07T14:30:00.000Z",
                        "fullName": "Bob Smith",
                        "email": "bob@example.com",
                        "institution": "MIT Media Lab",
                        "phone": "+1-555-9999",
                        "attendee": {
                            "id": "usr-888",
                            "fullName": "Bob Smith",
                            "email": "bob@example.com",
                            "institution": "MIT Media Lab",
                            "phone": "+1-555-9999",
                            "organization": "MIT",
                            "title": "Graduate Student",
                            "avatarUrl": null
                        },
                        "answers": [
                            {
                                "id": "ans-99",
                                "questionId": "q-laptop",
                                "questionLabel": "Bringing Laptop?",
                                "questionType": "MULTIPLE_CHOICE",
                                "value": "Yes",
                                "options": ["Yes", "No"],
                                "orderIndex": 3,
                                "isDefaultField": false
                            }
                        ],
                        "event": {
                            "id": "ev-101",
                            "title": "Cloud DevCon 2026",
                            "slug": "cloud-devcon-2026"
                        }
                    }
                },
                "timestamp": "2026-09-07T15:00:00Z"
            }
        """.trimIndent()

        server.enqueue(MockResponse().setResponseCode(200).setBody(json))

        val result = repository.getRegistrationDetail("reg-999")
        assertTrue(result.isSuccess)

        val reg = result.getOrNull()
        assertNotNull(reg)
        assertEquals("reg-999", reg!!.id)
        assertEquals("Bob Smith", reg.attendee?.fullName)
        assertEquals("bob@example.com", reg.attendee?.email)
        assertEquals("MIT", reg.attendee?.organization)
        assertEquals("Graduate Student", reg.attendee?.title)
        assertEquals("NY-PRO-99", reg.vehicleNumber)
        assertEquals(1, reg.answers.size)
        assertEquals("Bringing Laptop?", reg.answers[0].questionLabel)
        assertEquals("Yes", reg.answers[0].value)
        assertEquals("Cloud DevCon 2026", reg.event?.title)

        val recordedRequest = server.takeRequest()
        assertEquals("GET", recordedRequest.method)
        assertEquals("/api/v1/organizer/registrations/reg-999", recordedRequest.path)
    }

    @Test
    fun getRegistrationDetail_not_found_404() = runBlocking {
        val json = """
            {
                "success": false,
                "error": "Not Found",
                "message": "Registration not found",
                "statusCode": 404
            }
        """.trimIndent()

        server.enqueue(MockResponse().setResponseCode(404).setBody(json))

        val result = repository.getRegistrationDetail("non-existent-reg")
        assertFalse(result.isSuccess)
        assertTrue(result.exceptionOrNull()?.message?.contains("Registration not found") == true)
    }

    @Test
    fun getRegistrationDetail_forbidden_403() = runBlocking {
        val json = """
            {
                "success": false,
                "error": "Forbidden",
                "message": "You do not have permission to view this registration",
                "statusCode": 403
            }
        """.trimIndent()

        server.enqueue(MockResponse().setResponseCode(403).setBody(json))

        val result = repository.getRegistrationDetail("reg-unauthorized")
        assertFalse(result.isSuccess)
        assertTrue(result.exceptionOrNull()?.message?.contains("permission") == true)
    }

    @Test
    fun toOrganizerEvent_maps_registrationCount_to_attendeeCount() {
        val dto = EventDto(
            id = "ev-456",
            slug = "ai-conf-2026",
            title = "AI Conf 2026",
            location = "San Francisco",
            startDate = "2026-10-15T09:00:00Z",
            endDate = "2026-10-15T18:00:00Z",
            registrationCount = 42
        )

        val organizerEvent = dto.toOrganizerEvent()
        assertEquals("ev-456", organizerEvent.id)
        assertEquals("AI Conf 2026", organizerEvent.name)
        assertEquals(42, organizerEvent.attendeeCount)
    }
}
