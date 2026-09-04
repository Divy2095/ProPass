package com.mpc.propass.network

import com.mpc.propass.network.model.ApiError
import com.mpc.propass.network.model.ApiResponse
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ApiResponseParsingTest {

    private lateinit var moshi: Moshi

    @Before
    fun setUp() {
        moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    @Test
    fun testParseSuccessEnvelope() {
        val json = """
            {
                "success": true,
                "message": "Operation completed successfully",
                "data": {
                    "event": {
                        "slug": "techconf-2024",
                        "title": "TechConf 2024"
                    }
                },
                "timestamp": "2026-08-29T15:00:00.000Z"
            }
        """.trimIndent()

        val type = Types.newParameterizedType(
            ApiResponse::class.java,
            Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
        )
        val adapter = moshi.adapter<ApiResponse<Map<String, Any>>>(type)
        val response = adapter.fromJson(json)

        assertNotNull(response)
        assertTrue(response!!.success)
        assertEquals("Operation completed successfully", response.message)
        assertNotNull(response.data)
        assertEquals("2026-08-29T15:00:00.000Z", response.timestamp)
        assertNull(response.error)
    }

    @Test
    fun testParseErrorEnvelope() {
        val json = """
            {
                "success": false,
                "error": "Bad Request",
                "message": "Invalid ProPass QR code format. Expected format: https://propass.id/event/<eventId>",
                "statusCode": 400,
                "timestamp": "2026-08-29T15:00:00.000Z"
            }
        """.trimIndent()

        val adapter = moshi.adapter(ApiError::class.java)
        val error = adapter.fromJson(json)

        assertNotNull(error)
        assertFalse(error!!.success)
        assertEquals("Bad Request", error.error)
        assertTrue(error.message!!.contains("Invalid ProPass QR code format"))
        assertEquals(400, error.statusCode)
        assertEquals("2026-08-29T15:00:00.000Z", error.timestamp)
    }

    @Test
    fun testParseValidationErrorWithDetails() {
        val json = """
            {
                "success": false,
                "error": "Validation Error",
                "message": "Duration exceeds allowed duration",
                "details": [
                    {
                        "field": "durationDays",
                        "message": "Duration (10 days) exceeds event maximum allowed duration (1 days)"
                    }
                ],
                "statusCode": 400,
                "timestamp": "2026-08-29T15:00:00.000Z"
            }
        """.trimIndent()

        val adapter = moshi.adapter(ApiError::class.java)
        val error = adapter.fromJson(json)

        assertNotNull(error)
        assertFalse(error!!.success)
        assertEquals("Validation Error", error.error)
        assertEquals(1, error.details?.size)
        assertEquals("durationDays", error.details?.get(0)?.field)
        assertTrue(error.details?.get(0)?.message?.contains("exceeds event maximum") == true)
    }
}
