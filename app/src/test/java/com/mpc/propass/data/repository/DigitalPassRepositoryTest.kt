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

class DigitalPassRepositoryTest {

    private lateinit var server: MockWebServer
    private lateinit var digitalPassRepository: DigitalPassRepository

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()

        val apiService = NetworkClient.createApiService(
            baseUrl = server.url("/").toString(),
            enableLogging = false
        )

        digitalPassRepository = DigitalPassRepositoryImpl(apiService = apiService)
    }

    @After
    fun tearDown() {
        server.shutdown()
        NetworkClient.reset()
    }

    @Test
    fun getMyDigitalPass_success_200() = runBlocking {
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
                        "expiresAt": "2026-12-31T23:59:59Z"
                    },
                    "holder": {
                        "userId": "usr-001",
                        "email": "elena.rodriguez@acmecorp.com",
                        "fullName": "Elena Rodriguez",
                        "title": "Lead Product Designer",
                        "organization": "Acme Corp",
                        "phone": "+1 (555) 234-5678",
                        "linkedinUrl": "https://linkedin.com/in/elenarodriguez",
                        "isVerified": true
                    }
                },
                "timestamp": "2026-09-04T12:00:00Z"
            }
        """.trimIndent()

        server.enqueue(MockResponse().setResponseCode(200).setBody(json))

        val result = digitalPassRepository.getMyDigitalPass()
        assertTrue(result.isSuccess)

        val data = result.getOrNull()
        assertNotNull(data)
        assertEquals("PP-2024-0042", data!!.pass.passNumber)
        assertEquals("PREMIUM", data.pass.tier)
        assertEquals("Elena Rodriguez", data.holder.fullName)
        assertEquals("Acme Corp", data.holder.organization)

        val recordedRequest = server.takeRequest()
        assertEquals("/api/v1/passes/me", recordedRequest.path)
        assertEquals("GET", recordedRequest.method)
    }

    @Test
    fun getMyDigitalPass_notFound_404() = runBlocking {
        val json = """
            {
                "success": false,
                "error": "Not Found",
                "message": "No digital pass found for this user account",
                "statusCode": 404,
                "timestamp": "2026-09-04T12:00:00Z"
            }
        """.trimIndent()

        server.enqueue(MockResponse().setResponseCode(404).setBody(json))

        val result = digitalPassRepository.getMyDigitalPass()
        assertFalse(result.isSuccess)
        assertTrue(result.exceptionOrNull() is NoActivePassException)
        assertEquals("No digital pass found for this user account", result.exceptionOrNull()?.message)
    }

    @Test
    fun getMyDigitalPass_unauthorized_401() = runBlocking {
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

        val result = digitalPassRepository.getMyDigitalPass()
        assertFalse(result.isSuccess)
        assertTrue(result.exceptionOrNull() is PassAuthException)
        assertEquals("Session expired or invalid token", result.exceptionOrNull()?.message)
    }

    @Test
    fun getMyDigitalPass_networkError() = runBlocking {
        server.shutdown()

        val result = digitalPassRepository.getMyDigitalPass()
        assertFalse(result.isSuccess)
        assertTrue(result.exceptionOrNull() is IOException)
    }
}
