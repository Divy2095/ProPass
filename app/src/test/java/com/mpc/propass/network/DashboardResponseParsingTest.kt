package com.mpc.propass.network

import com.mpc.propass.network.model.ApiResponse
import com.mpc.propass.network.model.DashboardResponseData
import com.squareup.moshi.Types
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DashboardResponseParsingTest {

    private val moshi = NetworkClient.moshi

    @Test
    fun parseDashboardResponse_fullData_success() {
        val json = """
            {
                "success": true,
                "data": {
                    "greeting": "Hello, Sarah",
                    "user": {
                        "id": "u-101",
                        "email": "sarah.jenkins@example.com",
                        "role": "USER"
                    },
                    "profile": {
                        "id": "p-101",
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
                        "id": "pass-101",
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
                        },
                        {
                            "registrationId": "reg-2",
                            "eventId": "e-2",
                            "eventSlug": "google-office-visit",
                            "eventTitle": "Google Office Visit",
                            "eventOverline": "Campus Visit",
                            "eventSubtitle": "Building 43",
                            "eventLocation": "Mountain View, CA",
                            "eventStartDate": "2024-10-24T14:00:00Z",
                            "purpose": "SPEAKER",
                            "durationDays": 1,
                            "status": "CHECKED_IN",
                            "registeredAt": "2026-08-20T14:00:00Z"
                        }
                    ]
                },
                "timestamp": "2026-09-04T12:00:00Z"
            }
        """.trimIndent()

        val type = Types.newParameterizedType(ApiResponse::class.java, DashboardResponseData::class.java)
        val adapter = moshi.adapter<ApiResponse<DashboardResponseData>>(type)
        val response = adapter.fromJson(json)

        assertNotNull(response)
        assertTrue(response!!.success)

        val data = response.data
        assertNotNull(data)
        assertEquals("Hello, Sarah", data!!.greeting)
        assertEquals("sarah.jenkins@example.com", data.user.email)
        assertEquals("Sarah Jenkins", data.profile?.fullName)
        assertEquals(85, data.profile?.completionScore)
        assertEquals("PP-2024-0042", data.pass?.passNumber)
        assertEquals("PREMIUM", data.pass?.tier)

        assertEquals(2, data.recentActivity.size)
        assertEquals("TechConf 2024", data.recentActivity[0].eventTitle)
        assertEquals("GENERAL_ATTENDEE", data.recentActivity[0].purpose)
        assertEquals("CONFIRMED", data.recentActivity[0].status)
        assertEquals("Google Office Visit", data.recentActivity[1].eventTitle)
    }

    @Test
    fun parseDashboardResponse_emptyActivity_nullPass_success() {
        val json = """
            {
                "success": true,
                "data": {
                    "greeting": "Hello, John",
                    "user": {
                        "id": "u-202",
                        "email": "john.doe@example.com",
                        "role": "USER"
                    },
                    "profile": null,
                    "pass": null,
                    "recentActivity": []
                },
                "timestamp": "2026-09-04T12:00:00Z"
            }
        """.trimIndent()

        val type = Types.newParameterizedType(ApiResponse::class.java, DashboardResponseData::class.java)
        val adapter = moshi.adapter<ApiResponse<DashboardResponseData>>(type)
        val response = adapter.fromJson(json)

        assertNotNull(response)
        assertTrue(response!!.success)

        val data = response.data
        assertNotNull(data)
        assertEquals("Hello, John", data!!.greeting)
        assertNull(data.profile)
        assertNull(data.pass)
        assertTrue(data.recentActivity.isEmpty())
    }
}
