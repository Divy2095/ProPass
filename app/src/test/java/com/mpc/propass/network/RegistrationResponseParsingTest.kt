package com.mpc.propass.network

import com.mpc.propass.network.model.ApiResponse
import com.mpc.propass.network.model.CreateRegistrationRequest
import com.mpc.propass.network.model.CreateRegistrationResponseData
import com.mpc.propass.network.model.MyRegistrationsResponseData
import com.mpc.propass.network.model.RegistrationPurposeMapper
import com.squareup.moshi.Types
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RegistrationResponseParsingTest {

    private val moshi = NetworkClient.moshi

    @Test
    fun serializeCreateRegistrationRequest_validJson() {
        val request = CreateRegistrationRequest(
            eventId = "techconf-2024",
            fullName = "Sarah Jenkins",
            email = "sarah.jenkins@example.com",
            institution = "TechFlow Inc.",
            purpose = "GENERAL_ATTENDEE",
            durationDays = 3,
            vehicleNumber = "CA-9876-TX"
        )

        val adapter = moshi.adapter(CreateRegistrationRequest::class.java)
        val json = adapter.toJson(request)

        assertTrue(json.contains("\"eventId\":\"techconf-2024\""))
        assertTrue(json.contains("\"fullName\":\"Sarah Jenkins\""))
        assertTrue(json.contains("\"email\":\"sarah.jenkins@example.com\""))
        assertTrue(json.contains("\"institution\":\"TechFlow Inc.\""))
        assertTrue(json.contains("\"purpose\":\"GENERAL_ATTENDEE\""))
        assertTrue(json.contains("\"durationDays\":3"))
        assertTrue(json.contains("\"vehicleNumber\":\"CA-9876-TX\""))
    }

    @Test
    fun parseCreateRegistrationResponse_success() {
        val json = """
            {
                "success": true,
                "message": "Registration confirmed successfully",
                "data": {
                    "registration": {
                        "id": "reg-001",
                        "userId": "usr-001",
                        "eventId": "evt-001",
                        "fullName": "Sarah Jenkins",
                        "email": "sarah.jenkins@example.com",
                        "institution": "TechFlow Inc.",
                        "purpose": "GENERAL_ATTENDEE",
                        "durationDays": 3,
                        "vehicleNumber": "CA-9876-TX",
                        "status": "CONFIRMED",
                        "registeredAt": "2026-09-04T12:00:00Z",
                        "event": {
                            "id": "evt-001",
                            "slug": "techconf-2024",
                            "title": "TechConf 2024",
                            "overline": "CONFERENCE BADGE",
                            "subtitle": "Annual Developer Summit",
                            "location": "San Francisco, CA",
                            "startDate": "2026-10-15T09:00:00Z",
                            "endDate": "2026-10-17T18:00:00Z"
                        }
                    }
                },
                "timestamp": "2026-09-04T12:00:00Z"
            }
        """.trimIndent()

        val type = Types.newParameterizedType(ApiResponse::class.java, CreateRegistrationResponseData::class.java)
        val adapter = moshi.adapter<ApiResponse<CreateRegistrationResponseData>>(type)
        val response = adapter.fromJson(json)

        assertNotNull(response)
        assertTrue(response!!.success)
        assertEquals("Registration confirmed successfully", response.message)

        val reg = response.data?.registration
        assertNotNull(reg)
        assertEquals("reg-001", reg!!.id)
        assertEquals("Sarah Jenkins", reg.fullName)
        assertEquals("sarah.jenkins@example.com", reg.email)
        assertEquals("TechFlow Inc.", reg.institution)
        assertEquals("GENERAL_ATTENDEE", reg.purpose)
        assertEquals(3, reg.durationDays)
        assertEquals("CA-9876-TX", reg.vehicleNumber)
        assertEquals("CONFIRMED", reg.status)
        assertEquals("TechConf 2024", reg.event?.title)
        assertEquals("techconf-2024", reg.event?.slug)
    }

    @Test
    fun parseMyRegistrationsResponse_success() {
        val json = """
            {
                "success": true,
                "data": {
                    "registrations": [
                        {
                            "id": "reg-002",
                            "userId": "usr-001",
                            "eventId": "evt-002",
                            "fullName": "Sarah Jenkins",
                            "email": "sarah.jenkins@example.com",
                            "institution": "TechFlow Inc.",
                            "purpose": "SPEAKER",
                            "durationDays": 1,
                            "vehicleNumber": null,
                            "status": "CONFIRMED",
                            "registeredAt": "2026-09-03T10:00:00Z",
                            "event": null
                        }
                    ],
                    "count": 1
                },
                "timestamp": "2026-09-04T12:00:00Z"
            }
        """.trimIndent()

        val type = Types.newParameterizedType(ApiResponse::class.java, MyRegistrationsResponseData::class.java)
        val adapter = moshi.adapter<ApiResponse<MyRegistrationsResponseData>>(type)
        val response = adapter.fromJson(json)

        assertNotNull(response)
        assertTrue(response!!.success)
        assertEquals(1, response.data?.count)
        assertEquals(1, response.data?.registrations?.size)

        val reg = response.data!!.registrations[0]
        assertEquals("reg-002", reg.id)
        assertEquals("SPEAKER", reg.purpose)
        assertNull(reg.vehicleNumber)
        assertNull(reg.event)
    }

    @Test
    fun testRegistrationPurposeMapper_allUiOptions() {
        // UI Friendly Strings to backend enum mapping
        assertEquals("GENERAL_ATTENDEE", RegistrationPurposeMapper.toBackendPurpose("General Attendee"))
        assertEquals("GENERAL_ATTENDEE", RegistrationPurposeMapper.toBackendPurpose("general attendee"))
        assertEquals("SPEAKER", RegistrationPurposeMapper.toBackendPurpose("Speaker"))
        assertEquals("SPEAKER", RegistrationPurposeMapper.toBackendPurpose("speaker"))
        assertEquals("SPONSOR_EXHIBITOR", RegistrationPurposeMapper.toBackendPurpose("Sponsor/Exhibitor"))
        assertEquals("SPONSOR_EXHIBITOR", RegistrationPurposeMapper.toBackendPurpose("Sponsor / Exhibitor"))
        assertEquals("SPONSOR_EXHIBITOR", RegistrationPurposeMapper.toBackendPurpose("Sponsor"))
        assertEquals("MEDIA_PRESS", RegistrationPurposeMapper.toBackendPurpose("Media/Press"))
        assertEquals("MEDIA_PRESS", RegistrationPurposeMapper.toBackendPurpose("Media / Press"))
        assertEquals("MEDIA_PRESS", RegistrationPurposeMapper.toBackendPurpose("Media"))
        assertEquals("GENERAL_ATTENDEE", RegistrationPurposeMapper.toBackendPurpose(null))

        // Backend enum to friendly display mapping
        assertEquals("General Attendee", RegistrationPurposeMapper.toFriendlyDisplay("GENERAL_ATTENDEE"))
        assertEquals("Speaker", RegistrationPurposeMapper.toFriendlyDisplay("SPEAKER"))
        assertEquals("Sponsor/Exhibitor", RegistrationPurposeMapper.toFriendlyDisplay("SPONSOR_EXHIBITOR"))
        assertEquals("Media/Press", RegistrationPurposeMapper.toFriendlyDisplay("MEDIA_PRESS"))
        assertEquals("General Attendee", RegistrationPurposeMapper.toFriendlyDisplay(null))
    }

    @Test
    fun parseRegistrationDetailResponse_withAnswersAndQuestionMeta() {
        val json = """
            {
                "success": true,
                "data": {
                    "registration": {
                        "id": "reg-999",
                        "userId": "usr-888",
                        "eventId": "evt-777",
                        "fullName": "Alice Walker",
                        "email": "alice@example.com",
                        "institution": "Stanford",
                        "purpose": "SPEAKER",
                        "durationDays": 2,
                        "vehicleNumber": "SF-9912",
                        "status": "CONFIRMED",
                        "registeredAt": "2026-09-08T12:00:00Z",
                        "event": {
                            "id": "evt-777",
                            "slug": "ai-con-2026",
                            "title": "AI Con 2026",
                            "description": "Annual AI Conference",
                            "location": "San Jose, CA",
                            "date": "2026-11-10",
                            "startTime": "09:00 AM",
                            "endTime": "05:00 PM"
                        },
                        "answers": [
                            {
                                "id": "ans-1",
                                "questionId": "q-diet",
                                "value": "Vegan",
                                "question": {
                                    "id": "q-diet",
                                    "label": "Dietary Requirements",
                                    "type": "SINGLE_CHOICE",
                                    "isRequired": true,
                                    "options": ["Vegan", "Vegetarian", "None"],
                                    "orderIndex": 0
                                }
                            }
                        ]
                    }
                },
                "timestamp": "2026-09-08T12:00:00Z"
            }
        """.trimIndent()

        val type = Types.newParameterizedType(ApiResponse::class.java, com.mpc.propass.network.model.RegistrationDetailResponseData::class.java)
        val adapter = moshi.adapter<ApiResponse<com.mpc.propass.network.model.RegistrationDetailResponseData>>(type)
        val response = adapter.fromJson(json)

        assertNotNull(response)
        assertTrue(response!!.success)
        val reg = response.data?.registration
        assertNotNull(reg)
        assertEquals("reg-999", reg!!.id)
        assertEquals("AI Con 2026", reg.event?.title)
        assertEquals("Annual AI Conference", reg.event?.description)
        assertEquals("San Jose, CA", reg.event?.location)
        assertEquals(1, reg.answers.size)
        assertEquals("Dietary Requirements", reg.answers[0].displayLabel)
        assertEquals("Vegan", reg.answers[0].value)
        assertTrue(reg.answers[0].isRequired)
    }
}
