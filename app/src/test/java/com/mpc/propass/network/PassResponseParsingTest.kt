package com.mpc.propass.network

import com.mpc.propass.network.model.ApiResponse
import com.mpc.propass.network.model.MyPassResponseData
import com.squareup.moshi.Types
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PassResponseParsingTest {

    private val moshi = NetworkClient.moshi

    @Test
    fun parseMyPassResponse_fullData_success() {
        val json = """
            {
                "success": true,
                "data": {
                    "pass": {
                        "id": "pass-001",
                        "passNumber": "PP-2024-0042",
                        "tier": "PREMIUM",
                        "isActive": true,
                        "isExpired": false,
                        "qrPayload": "propass:pass:PP-2024-0042",
                        "expiresAt": "2026-12-31T23:59:59Z",
                        "createdAt": "2026-09-01T00:00:00Z",
                        "updatedAt": "2026-09-02T00:00:00Z"
                    },
                    "holder": {
                        "userId": "usr-001",
                        "email": "elena.rodriguez@acmecorp.com",
                        "fullName": "Elena Rodriguez",
                        "title": "Lead Product Designer",
                        "organization": "Acme Corp",
                        "phone": "+1 (555) 234-5678",
                        "linkedinUrl": "https://linkedin.com/in/elenarodriguez",
                        "avatarUrl": "https://propass.id/avatars/elena.jpg",
                        "isVerified": true
                    }
                },
                "timestamp": "2026-09-04T12:00:00Z"
            }
        """.trimIndent()

        val type = Types.newParameterizedType(ApiResponse::class.java, MyPassResponseData::class.java)
        val adapter = moshi.adapter<ApiResponse<MyPassResponseData>>(type)
        val response = adapter.fromJson(json)

        assertNotNull(response)
        assertTrue(response!!.success)

        val data = response.data
        assertNotNull(data)

        val pass = data!!.pass
        assertEquals("pass-001", pass.id)
        assertEquals("PP-2024-0042", pass.passNumber)
        assertEquals("PREMIUM", pass.tier)
        assertTrue(pass.isActive)
        assertFalse(pass.isExpired)
        assertEquals("propass:pass:PP-2024-0042", pass.qrPayload)
        assertEquals("2026-12-31T23:59:59Z", pass.expiresAt)

        val holder = data.holder
        assertEquals("usr-001", holder.userId)
        assertEquals("elena.rodriguez@acmecorp.com", holder.email)
        assertEquals("Elena Rodriguez", holder.fullName)
        assertEquals("Lead Product Designer", holder.title)
        assertEquals("Acme Corp", holder.organization)
        assertEquals("+1 (555) 234-5678", holder.phone)
        assertEquals("https://linkedin.com/in/elenarodriguez", holder.linkedinUrl)
        assertTrue(holder.isVerified)
    }

    @Test
    fun parseMyPassResponse_nullableHolderFields_success() {
        val json = """
            {
                "success": true,
                "data": {
                    "pass": {
                        "id": "pass-002",
                        "passNumber": "PP-2024-0099",
                        "tier": "STANDARD",
                        "isActive": false,
                        "isExpired": true,
                        "qrPayload": "propass:pass:PP-2024-0099",
                        "expiresAt": null
                    },
                    "holder": {
                        "userId": "usr-002",
                        "email": "newuser@example.com",
                        "fullName": "New User",
                        "title": null,
                        "organization": null,
                        "phone": null,
                        "linkedinUrl": null,
                        "avatarUrl": null,
                        "isVerified": false
                    }
                },
                "timestamp": "2026-09-04T12:00:00Z"
            }
        """.trimIndent()

        val type = Types.newParameterizedType(ApiResponse::class.java, MyPassResponseData::class.java)
        val adapter = moshi.adapter<ApiResponse<MyPassResponseData>>(type)
        val response = adapter.fromJson(json)

        assertNotNull(response)
        assertTrue(response!!.success)

        val data = response.data
        assertNotNull(data)
        assertFalse(data!!.pass.isActive)
        assertTrue(data.pass.isExpired)
        assertNull(data.pass.expiresAt)

        val holder = data.holder
        assertNull(holder.title)
        assertNull(holder.organization)
        assertNull(holder.phone)
        assertNull(holder.linkedinUrl)
        assertFalse(holder.isVerified)
    }
}
