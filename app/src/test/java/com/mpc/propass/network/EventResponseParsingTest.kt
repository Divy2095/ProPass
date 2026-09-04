package com.mpc.propass.network

import com.mpc.propass.network.model.ApiResponse
import com.mpc.propass.network.model.EventDto
import com.mpc.propass.network.model.EventResponseData
import com.mpc.propass.network.model.ValidateQrRequest
import com.mpc.propass.network.model.ValidateQrResponseData
import com.squareup.moshi.Types
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EventResponseParsingTest {

    private val moshi = NetworkClient.moshi

    @Test
    fun parseValidateQrResponse_success() {
        val json = """
            {
                "success": true,
                "message": "QR code validated successfully",
                "data": {
                    "event": {
                        "id": "e-101",
                        "slug": "techconf-2024",
                        "title": "TechConf 2024",
                        "overline": "CONFERENCE BADGE",
                        "subtitle": "Annual Developer Summit",
                        "description": "The premier gathering for software engineers.",
                        "location": "Moscone Center, San Francisco, CA",
                        "startDate": "2026-10-15T09:00:00Z",
                        "endDate": "2026-10-17T18:00:00Z",
                        "maxDuration": 3,
                        "isActive": true,
                        "createdAt": "2026-09-01T00:00:00Z",
                        "updatedAt": "2026-09-02T00:00:00Z"
                    },
                    "qrPayload": "https://propass.id/event/techconf-2024",
                    "parsedSlug": "techconf-2024"
                },
                "timestamp": "2026-09-04T12:00:00Z"
            }
        """.trimIndent()

        val type = Types.newParameterizedType(ApiResponse::class.java, ValidateQrResponseData::class.java)
        val adapter = moshi.adapter<ApiResponse<ValidateQrResponseData>>(type)
        val response = adapter.fromJson(json)

        assertNotNull(response)
        assertTrue(response!!.success)
        assertEquals("QR code validated successfully", response.message)

        val data = response.data
        assertNotNull(data)
        assertEquals("techconf-2024", data!!.parsedSlug)
        assertEquals("https://propass.id/event/techconf-2024", data.qrPayload)

        val event = data.event
        assertEquals("e-101", event.id)
        assertEquals("techconf-2024", event.slug)
        assertEquals("TechConf 2024", event.title)
        assertEquals("CONFERENCE BADGE", event.overline)
        assertEquals("Annual Developer Summit", event.subtitle)
        assertEquals("Moscone Center, San Francisco, CA", event.location)
        assertEquals(3, event.maxDuration)
        assertTrue(event.isActive)
    }

    @Test
    fun parseEventResponse_nullableFields_success() {
        val json = """
            {
                "success": true,
                "data": {
                    "event": {
                        "id": "e-102",
                        "slug": "simple-meetup",
                        "title": "Simple Meetup",
                        "overline": null,
                        "subtitle": null,
                        "description": null,
                        "location": "Room 404",
                        "startDate": "2026-11-01T10:00:00Z",
                        "endDate": "2026-11-01T12:00:00Z",
                        "maxDuration": 1,
                        "isActive": false
                    }
                },
                "timestamp": "2026-09-04T12:00:00Z"
            }
        """.trimIndent()

        val type = Types.newParameterizedType(ApiResponse::class.java, EventResponseData::class.java)
        val adapter = moshi.adapter<ApiResponse<EventResponseData>>(type)
        val response = adapter.fromJson(json)

        assertNotNull(response)
        assertTrue(response!!.success)

        val event = response.data?.event
        assertNotNull(event)
        assertEquals("e-102", event!!.id)
        assertEquals("simple-meetup", event.slug)
        assertEquals("Simple Meetup", event.title)
        assertNull(event.overline)
        assertNull(event.subtitle)
        assertNull(event.description)
        assertEquals("Room 404", event.location)
        assertEquals(1, event.maxDuration)
        assertFalse(event.isActive)
    }

    @Test
    fun serializeValidateQrRequest_validJson() {
        val request = ValidateQrRequest(qrContent = "https://propass.id/event/google-office-visit")
        val adapter = moshi.adapter(ValidateQrRequest::class.java)
        val json = adapter.toJson(request)

        assertTrue(json.contains("\"qrContent\":\"https://propass.id/event/google-office-visit\""))
    }
}
