package com.mpc.propass.organizer

import com.mpc.propass.network.NetworkClient
import com.mpc.propass.network.model.CreateEventRequest
import com.mpc.propass.organizer.data.OrganizerEventRepository
import com.mpc.propass.organizer.data.OrganizerEventRepositoryImpl
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

class OrganizerEventRepositoryTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: OrganizerEventRepository

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()

        val apiService = NetworkClient.createApiService(
            baseUrl = server.url("/").toString(),
            enableLogging = false
        )
        repository = OrganizerEventRepositoryImpl(apiService = apiService)
    }

    @After
    fun tearDown() {
        server.shutdown()
        NetworkClient.reset()
    }

    @Test
    fun createEvent_success_201() = runBlocking {
        val json = """
            {
                "success": true,
                "message": "Event created successfully",
                "data": {
                    "event": {
                        "id": "ev-123",
                        "slug": "ai-summit-2026",
                        "title": "AI Summit 2026",
                        "description": "Annual AI Conference",
                        "date": "2026-10-15",
                        "startTime": "09:00 AM",
                        "endTime": "05:00 PM",
                        "location": "Convention Center",
                        "startDate": "2026-10-15T09:00:00Z",
                        "endDate": "2026-10-15T17:00:00Z",
                        "maxDuration": 1,
                        "isActive": true,
                        "organizerId": "user-org-1",
                        "qrPayload": "https://propass.id/event/ai-summit-2026"
                    }
                },
                "timestamp": "2026-09-07T12:00:00Z"
            }
        """.trimIndent()

        server.enqueue(MockResponse().setResponseCode(201).setBody(json))

        val request = CreateEventRequest(
            name = "AI Summit 2026",
            description = "Annual AI Conference",
            date = "2026-10-15",
            startTime = "09:00 AM",
            endTime = "05:00 PM",
            location = "Convention Center",
            maxDuration = 1
        )

        val result = repository.createEvent(request)

        assertTrue(result.isSuccess)
        val event = result.getOrThrow()
        assertEquals("ev-123", event.id)
        assertEquals("ai-summit-2026", event.slug)
        assertEquals("AI Summit 2026", event.title)
        assertEquals("Convention Center", event.location)
        assertEquals("https://propass.id/event/ai-summit-2026", event.qrPayload)

        val recorded = server.takeRequest()
        assertEquals("POST", recorded.method)
        assertEquals("/api/v1/events", recorded.path)
        assertTrue(recorded.body.readUtf8().contains("AI Summit 2026"))
    }

    @Test
    fun createEvent_validationError_400() = runBlocking {
        val json = """
            {
                "success": false,
                "error": "Validation Error",
                "message": "Invalid date format. Expected YYYY-MM-DD",
                "statusCode": 400
            }
        """.trimIndent()

        server.enqueue(MockResponse().setResponseCode(400).setBody(json))

        val request = CreateEventRequest(
            name = "Bad Event",
            date = "invalid-date",
            location = "Nowhere"
        )

        val result = repository.createEvent(request)

        assertFalse(result.isSuccess)
        val exception = result.exceptionOrNull()
        assertNotNull(exception)
        assertTrue(exception?.message?.contains("Invalid date format") == true)
    }

    @Test
    fun createEvent_unauthorized_401() = runBlocking {
        val json = """
            {
                "success": false,
                "error": "Unauthorized",
                "message": "Authentication required",
                "statusCode": 401
            }
        """.trimIndent()

        server.enqueue(MockResponse().setResponseCode(401).setBody(json))

        val request = CreateEventRequest(
            name = "Unauthorized Event",
            date = "2026-10-15",
            location = "Center"
        )

        val result = repository.createEvent(request)

        assertFalse(result.isSuccess)
        val exception = result.exceptionOrNull()
        assertNotNull(exception)
        assertTrue(exception?.message?.contains("Authentication required") == true)
    }

    @Test
    fun createEvent_forbidden_403() = runBlocking {
        val json = """
            {
                "success": false,
                "error": "Forbidden",
                "message": "Organizer access required",
                "statusCode": 403
            }
        """.trimIndent()

        server.enqueue(MockResponse().setResponseCode(403).setBody(json))

        val request = CreateEventRequest(
            name = "Forbidden Event",
            date = "2026-10-15",
            location = "Center"
        )

        val result = repository.createEvent(request)

        assertFalse(result.isSuccess)
        val exception = result.exceptionOrNull()
        assertNotNull(exception)
        assertTrue(exception?.message?.contains("Organizer access required") == true)
    }

    @Test
    fun getMyEvents_success_200() = runBlocking {
        val json = """
            {
                "success": true,
                "data": {
                    "events": [
                        {
                            "id": "ev-1",
                            "slug": "event-1",
                            "title": "First Event",
                            "description": "First Desc",
                            "location": "Loc 1",
                            "startDate": "2026-10-01T00:00:00Z",
                            "endDate": "2026-10-01T23:59:59Z",
                            "maxDuration": 1,
                            "isActive": true,
                            "qrPayload": "https://propass.id/event/event-1"
                        },
                        {
                            "id": "ev-2",
                            "slug": "event-2",
                            "title": "Second Event",
                            "description": "Second Desc",
                            "location": "Loc 2",
                            "startDate": "2026-11-01T00:00:00Z",
                            "endDate": "2026-11-01T23:59:59Z",
                            "maxDuration": 2,
                            "isActive": true,
                            "qrPayload": "https://propass.id/event/event-2"
                        }
                    ]
                },
                "timestamp": "2026-09-07T12:00:00Z"
            }
        """.trimIndent()

        server.enqueue(MockResponse().setResponseCode(200).setBody(json))

        val result = repository.getMyEvents()

        assertTrue(result.isSuccess)
        val events = result.getOrThrow()
        assertEquals(2, events.size)
        assertEquals("First Event", events[0].title)
        assertEquals("Second Event", events[1].title)

        val recorded = server.takeRequest()
        assertEquals("GET", recorded.method)
        assertEquals("/api/v1/organizer/events", recorded.path)
    }

    @Test
    fun getMyEvents_emptyList_200() = runBlocking {
        val json = """
            {
                "success": true,
                "data": {
                    "events": []
                },
                "timestamp": "2026-09-07T12:00:00Z"
            }
        """.trimIndent()

        server.enqueue(MockResponse().setResponseCode(200).setBody(json))

        val result = repository.getMyEvents()

        assertTrue(result.isSuccess)
        val events = result.getOrThrow()
        assertTrue(events.isEmpty())
    }

    @Test
    fun getMyEvents_serverError_500() = runBlocking {
        val json = """
            {
                "success": false,
                "error": "Error",
                "message": "Database query failed",
                "statusCode": 500
            }
        """.trimIndent()

        server.enqueue(MockResponse().setResponseCode(500).setBody(json))

        val result = repository.getMyEvents()

        assertFalse(result.isSuccess)
        val exception = result.exceptionOrNull()
        assertNotNull(exception)
        assertTrue(exception?.message?.contains("Database query failed") == true)
    }

    @Test
    fun toOrganizerEvent_mapsEventDtoCorrectly() {
        val dto = com.mpc.propass.network.model.EventDto(
            id = "dto-99",
            slug = "mapped-event-99",
            title = "Mapped Event",
            description = "Mapped Desc",
            date = "2026-12-01",
            startTime = "10:00 AM",
            endTime = "02:00 PM",
            location = "Hall A",
            maxDuration = 2,
            isActive = true,
            qrPayload = "https://propass.id/event/mapped-event-99"
        )

        val orgEvent = dto.toOrganizerEvent()

        assertEquals("dto-99", orgEvent.id)
        assertEquals("mapped-event-99", orgEvent.slug)
        assertEquals("Mapped Event", orgEvent.name)
        assertEquals("Mapped Desc", orgEvent.description)
        assertEquals("2026-12-01", orgEvent.date)
        assertEquals("10:00 AM", orgEvent.startTime)
        assertEquals("02:00 PM", orgEvent.endTime)
        assertEquals("Hall A", orgEvent.location)
        assertEquals(2, orgEvent.maxDurationDays)
        assertEquals("PUBLISHED", orgEvent.status)
        assertEquals("https://propass.id/event/mapped-event-99", orgEvent.qrPayload)
        assertEquals(3, orgEvent.questions.size) // Default 3 fields
    }
}
