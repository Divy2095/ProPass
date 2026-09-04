package com.mpc.propass.network

import com.mpc.propass.network.model.ApiResponse
import com.mpc.propass.network.model.UpdateProfileRequest
import com.mpc.propass.network.model.UserProfileResponseData
import com.squareup.moshi.Types
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UserResponseParsingTest {

    private val moshi = NetworkClient.moshi

    @Test
    fun parseUserProfileResponse_success() {
        val json = """
            {
                "success": true,
                "data": {
                    "user": {
                        "id": "u-123",
                        "email": "sarah.jenkins@example.com",
                        "role": "USER",
                        "createdAt": "2026-09-04T12:00:00Z"
                    },
                    "profile": {
                        "id": "p-123",
                        "userId": "u-123",
                        "fullName": "Sarah Jenkins",
                        "title": "Senior UX Researcher",
                        "organization": "TechFlow Inc.",
                        "phone": "+1 (555) 018-9234",
                        "linkedinUrl": "https://linkedin.com/in/sarahjenkins",
                        "avatarUrl": "https://propass.id/avatars/sarah.jpg",
                        "isVerified": true,
                        "completionScore": 90,
                        "createdAt": "2026-09-04T12:00:00Z",
                        "updatedAt": "2026-09-04T12:30:00Z"
                    }
                },
                "timestamp": "2026-09-04T12:30:00Z"
            }
        """.trimIndent()

        val type = Types.newParameterizedType(ApiResponse::class.java, UserProfileResponseData::class.java)
        val adapter = moshi.adapter<ApiResponse<UserProfileResponseData>>(type)
        val response = adapter.fromJson(json)

        assertNotNull(response)
        assertTrue(response!!.success)
        assertEquals("sarah.jenkins@example.com", response.data?.user?.email)
        assertEquals("Sarah Jenkins", response.data?.profile?.fullName)
        assertEquals("Senior UX Researcher", response.data?.profile?.title)
        assertEquals("TechFlow Inc.", response.data?.profile?.organization)
        assertEquals(90, response.data?.profile?.completionScore)
        assertTrue(response.data?.profile?.isVerified == true)
    }

    @Test
    fun parseUserProfileResponse_nullProfile_success() {
        val json = """
            {
                "success": true,
                "data": {
                    "user": {
                        "id": "u-456",
                        "email": "new.user@example.com",
                        "role": "USER"
                    },
                    "profile": null
                },
                "timestamp": "2026-09-04T12:00:00Z"
            }
        """.trimIndent()

        val type = Types.newParameterizedType(ApiResponse::class.java, UserProfileResponseData::class.java)
        val adapter = moshi.adapter<ApiResponse<UserProfileResponseData>>(type)
        val response = adapter.fromJson(json)

        assertNotNull(response)
        assertTrue(response!!.success)
        assertEquals("new.user@example.com", response.data?.user?.email)
        assertNull(response.data?.profile)
    }

    @Test
    fun serializeUpdateProfileRequest_validJson() {
        val request = UpdateProfileRequest(
            fullName = "Alex Morgan",
            title = "Android Architect",
            organization = "ProPass Org",
            phone = "+1234567890",
            linkedinUrl = "https://linkedin.com/in/alex",
            avatarUrl = null
        )

        val adapter = moshi.adapter(UpdateProfileRequest::class.java)
        val json = adapter.toJson(request)

        assertTrue(json.contains("\"fullName\":\"Alex Morgan\""))
        assertTrue(json.contains("\"title\":\"Android Architect\""))
        assertTrue(json.contains("\"organization\":\"ProPass Org\""))
        assertTrue(json.contains("\"phone\":\"+1234567890\""))
    }
}
