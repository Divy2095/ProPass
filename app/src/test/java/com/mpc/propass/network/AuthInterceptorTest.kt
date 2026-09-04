package com.mpc.propass.network

import com.mpc.propass.network.interceptor.AuthInterceptor
import com.mpc.propass.network.interceptor.InMemoryTokenProvider
import com.mpc.propass.network.interceptor.NoOpTokenProvider
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class AuthInterceptorTest {

    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun testNoOpTokenProviderDoesNotAddAuthorizationHeader() {
        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(NoOpTokenProvider))
            .build()

        server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))

        val request = Request.Builder()
            .url(server.url("/test"))
            .build()

        client.newCall(request).execute()

        val recordedRequest = server.takeRequest()
        assertNull(recordedRequest.getHeader("Authorization"))
        assertEquals("application/json", recordedRequest.getHeader("Accept"))
    }

    @Test
    fun testTokenProviderInjectsBearerToken() {
        val tokenProvider = InMemoryTokenProvider("test-sample-jwt-token-12345")
        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(tokenProvider))
            .build()

        server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))

        val request = Request.Builder()
            .url(server.url("/test"))
            .build()

        client.newCall(request).execute()

        val recordedRequest = server.takeRequest()
        assertEquals("Bearer test-sample-jwt-token-12345", recordedRequest.getHeader("Authorization"))
        assertEquals("application/json", recordedRequest.getHeader("Accept"))
    }

    @Test
    fun testExplicitAuthorizationHeaderIsNotOverwritten() {
        val tokenProvider = InMemoryTokenProvider("auto-injected-token")
        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(tokenProvider))
            .build()

        server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))

        val request = Request.Builder()
            .url(server.url("/test"))
            .header("Authorization", "Basic custom-auth-header")
            .build()

        client.newCall(request).execute()

        val recordedRequest = server.takeRequest()
        assertEquals("Basic custom-auth-header", recordedRequest.getHeader("Authorization"))
    }
}
