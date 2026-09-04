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

class EventRepositoryTest {

    private lateinit var server: MockWebServer
    private lateinit var eventRepository: EventRepository

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()

        val apiService = NetworkClient.createApiService(
            baseUrl = server.url("/").toString(),
            enableLogging = false
        )

        eventRepository = EventRepositoryImpl(apiService = apiService)
    }

    @After
    fun tearDown() {
        server.shutdown()
        NetworkClient.reset()
    }

    @Test
    fun validateQr_success_200() = runBlocking {
        val json = """
            {
                "success": true,
                "message": "QR code validated successfully",
                "data": {
                    "event": {
                        "id": "e-1",
                        "slug": "techconf-2024",
                        "title": "TechConf 2024",
                        "overline": "CONFERENCE BADGE",
                        "subtitle": "Annual Developer Summit",
                        "description": "The premier gathering for software engineers.",
                        "location": "Moscone Center, San Francisco, CA",
                        "startDate": "2026-10-15T09:00:00Z",
                        "endDate": "2026-10-17T18:00:00Z",
                        "maxDuration": 3,
                        "isActive": true
                    },
                    "qrPayload": "https://propass.id/event/techconf-2024",
                    "parsedSlug": "techconf-2024"
                },
                "timestamp": "2026-09-04T12:00:00Z"
            }
        """.trimIndent()

        server.enqueue(MockResponse().setResponseCode(200).setBody(json))

        val result = eventRepository.validateQr("https://propass.id/event/techconf-2024")
        assertTrue(result.isSuccess)

        val data = result.getOrNull()
        assertNotNull(data)
        assertEquals("techconf-2024", data!!.event.slug)
        assertEquals("TechConf 2024", data.event.title)
        assertEquals(3, data.event.maxDuration)

        val recordedRequest = server.takeRequest()
        assertEquals("/api/v1/events/validate-qr", recordedRequest.path)
        assertEquals("POST", recordedRequest.method)
        assertTrue(recordedRequest.body.readUtf8().contains("techconf-2024"))
    }

    @Test
    fun validateQr_invalidFormat_400() = runBlocking {
        val json = """
            {
                "success": false,
                "error": "Invalid QR Format",
                "message": "The provided QR code is not a valid ProPass event URL.",
                "statusCode": 400,
                "timestamp": "2026-09-04T12:00:00Z"
            }
        """.trimIndent()

        server.enqueue(MockResponse().setResponseCode(400).setBody(json))

        val result = eventRepository.validateQr("https://google.com")
        assertFalse(result.isSuccess)
        assertTrue(result.exceptionOrNull() is InvalidQrException)
        assertEquals("The provided QR code is not a valid ProPass event URL.", result.exceptionOrNull()?.message)
    }

    @Test
    fun validateQr_eventNotFound_404() = runBlocking {
        val json = """
            {
                "success": false,
                "error": "Event Not Found",
                "message": "No active event found matching slug: nonexistent-event",
                "statusCode": 404,
                "timestamp": "2026-09-04T12:00:00Z"
            }
        """.trimIndent()

        server.enqueue(MockResponse().setResponseCode(404).setBody(json))

        val result = eventRepository.validateQr("https://propass.id/event/nonexistent-event")
        assertFalse(result.isSuccess)
        assertTrue(result.exceptionOrNull() is EventNotFoundException)
        assertEquals("No active event found matching slug: nonexistent-event", result.exceptionOrNull()?.message)
    }

    @Test
    fun validateQr_eventInactive_410() = runBlocking {
        val json = """
            {
                "success": false,
                "error": "Event Inactive",
                "message": "This event is no longer active or has concluded",
                "statusCode": 410,
                "timestamp": "2026-09-04T12:00:00Z"
            }
        """.trimIndent()

        server.enqueue(MockResponse().setResponseCode(410).setBody(json))

        val result = eventRepository.validateQr("https://propass.id/event/old-event")
        assertFalse(result.isSuccess)
        assertTrue(result.exceptionOrNull() is EventInactiveException)
        assertEquals("This event is no longer active or has concluded", result.exceptionOrNull()?.message)
    }

    @Test
    fun validateQr_emptyInput() = runBlocking {
        val result = eventRepository.validateQr("   ")
        assertFalse(result.isSuccess)
        assertTrue(result.exceptionOrNull() is InvalidQrException)
        assertEquals("QR content cannot be empty", result.exceptionOrNull()?.message)
    }

    @Test
    fun validateQr_networkError() = runBlocking {
        server.shutdown()

        val result = eventRepository.validateQr("https://propass.id/event/techconf-2024")
        assertFalse(result.isSuccess)
        assertTrue(result.exceptionOrNull() is IOException)
    }

    @Test
    fun getEvent_success_200() = runBlocking {
        val json = """
            {
                "success": true,
                "data": {
                    "event": {
                        "id": "e-2",
                        "slug": "google-office-visit",
                        "title": "Google Office Visit",
                        "overline": "VISITOR PASS",
                        "subtitle": "Campus Tour & Briefing",
                        "location": "Mountain View, CA",
                        "startDate": "2026-10-20T10:00:00Z",
                        "endDate": "2026-10-20T17:00:00Z",
                        "maxDuration": 1,
                        "isActive": true
                    }
                },
                "timestamp": "2026-09-04T12:00:00Z"
            }
        """.trimIndent()

        server.enqueue(MockResponse().setResponseCode(200).setBody(json))

        val result = eventRepository.getEvent("google-office-visit")
        assertTrue(result.isSuccess)

        val event = result.getOrNull()
        assertNotNull(event)
        assertEquals("google-office-visit", event!!.slug)
        assertEquals("Google Office Visit", event.title)
        assertEquals("Mountain View, CA", event.location)

        val recordedRequest = server.takeRequest()
        assertEquals("/api/v1/events/google-office-visit", recordedRequest.path)
        assertEquals("GET", recordedRequest.method)
    }

    @Test
    fun getEvent_notFound_404() = runBlocking {
        val json = """
            {
                "success": false,
                "error": "Event Not Found",
                "message": "Event not found with ID or slug: unknown-id",
                "statusCode": 404,
                "timestamp": "2026-09-04T12:00:00Z"
            }
        """.trimIndent()

        server.enqueue(MockResponse().setResponseCode(404).setBody(json))

        val result = eventRepository.getEvent("unknown-id")
        assertFalse(result.isSuccess)
        assertTrue(result.exceptionOrNull() is EventNotFoundException)
        assertEquals("Event not found with ID or slug: unknown-id", result.exceptionOrNull()?.message)
    }

    @Test
    fun getEvent_inactive_410() = runBlocking {
        val json = """
            {
                "success": false,
                "error": "Event Inactive",
                "message": "Event has concluded",
                "statusCode": 410,
                "timestamp": "2026-09-04T12:00:00Z"
            }
        """.trimIndent()

        server.enqueue(MockResponse().setResponseCode(410).setBody(json))

        val result = eventRepository.getEvent("concluded-event")
        assertFalse(result.isSuccess)
        assertTrue(result.exceptionOrNull() is EventInactiveException)
        assertEquals("Event has concluded", result.exceptionOrNull()?.message)
    }

    @Test
    fun getEvent_emptyId() = runBlocking {
        val result = eventRepository.getEvent("  ")
        assertFalse(result.isSuccess)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun getEvent_networkError() = runBlocking {
        server.shutdown()

        val result = eventRepository.getEvent("techconf-2024")
        assertFalse(result.isSuccess)
        assertTrue(result.exceptionOrNull() is IOException)
    }
}
