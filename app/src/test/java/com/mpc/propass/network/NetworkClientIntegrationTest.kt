package com.mpc.propass.network

import com.mpc.propass.network.interceptor.InMemoryTokenProvider
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class NetworkClientIntegrationTest {

    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
        NetworkClient.reset()
    }

    @Test
    fun testGetHealthSuccess() = runBlocking {
        val json = """
            {
                "success": true,
                "message": "System operational",
                "data": {
                    "status": "ok",
                    "database": "connected"
                },
                "timestamp": "2026-08-29T15:00:00.000Z"
            }
        """.trimIndent()

        server.enqueue(MockResponse().setResponseCode(200).setBody(json))

        val service = NetworkClient.createApiService(
            baseUrl = server.url("/").toString(),
            enableLogging = false
        )

        val response = service.getHealth()
        assertTrue(response.isSuccessful)
        assertEquals(200, response.code())

        val body = response.body()
        assertNotNull(body)
        assertTrue(body!!.success)
        assertEquals("System operational", body.message)
        assertEquals("ok", body.data?.get("status"))
        assertEquals("connected", body.data?.get("database"))

        val recordedRequest = server.takeRequest()
        assertEquals("/health", recordedRequest.path)
        assertEquals("application/json", recordedRequest.getHeader("Accept"))
    }

    @Test
    fun testAuthenticatedCallSendsBearerToken() = runBlocking {
        val json = """
            {
                "success": true,
                "data": {
                    "user": {
                        "email": "alex.morgan@example.com"
                    }
                },
                "timestamp": "2026-08-29T15:00:00.000Z"
            }
        """.trimIndent()

        server.enqueue(MockResponse().setResponseCode(200).setBody(json))

        val tokenProvider = InMemoryTokenProvider("jwt-secret-session-token-999")
        val service = NetworkClient.createApiService(
            baseUrl = server.url("/").toString(),
            tokenProvider = tokenProvider,
            enableLogging = false
        )

        val response = service.getCurrentUser()
        assertTrue(response.isSuccessful)

        val recordedRequest = server.takeRequest()
        assertEquals("/api/v1/auth/me", recordedRequest.path)
        assertEquals("Bearer jwt-secret-session-token-999", recordedRequest.getHeader("Authorization"))
    }
}
