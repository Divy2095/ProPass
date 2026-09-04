package com.mpc.propass.data.repository

import com.mpc.propass.data.local.FakeTokenStorage
import com.mpc.propass.network.NetworkClient
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AuthRepositoryTest {

    private lateinit var server: MockWebServer
    private lateinit var tokenStorage: FakeTokenStorage
    private lateinit var authRepository: AuthRepository

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()

        tokenStorage = FakeTokenStorage()
        val apiService = NetworkClient.createApiService(
            baseUrl = server.url("/").toString(),
            enableLogging = false
        )

        authRepository = AuthRepositoryImpl(
            apiService = apiService,
            tokenStorage = tokenStorage
        )
    }

    @After
    fun tearDown() {
        server.shutdown()
        NetworkClient.reset()
    }

    @Test
    fun testLoginSuccessSavesTokensAndUser() = runBlocking {
        val json = """
            {
                "success": true,
                "message": "Login successful",
                "data": {
                    "user": {
                        "id": "usr-12345",
                        "email": "alex.morgan@example.com",
                        "role": "USER",
                        "createdAt": "2026-08-29T15:00:00.000Z"
                    },
                    "tokens": {
                        "accessToken": "sample-access-jwt-token",
                        "refreshToken": "sample-refresh-token",
                        "expiresIn": "15m"
                    }
                },
                "timestamp": "2026-08-29T15:00:00.000Z"
            }
        """.trimIndent()

        server.enqueue(MockResponse().setResponseCode(200).setBody(json))

        val result = authRepository.login("alex.morgan@example.com", "Password123!")
        assertTrue(result.isSuccess)

        val data = result.getOrNull()
        assertEquals("usr-12345", data?.user?.id)
        assertEquals("sample-access-jwt-token", data?.tokens?.accessToken)

        // Verify tokens persisted
        assertEquals("sample-access-jwt-token", tokenStorage.getAccessToken())
        assertEquals("sample-refresh-token", tokenStorage.getRefreshToken())
        assertEquals("usr-12345", tokenStorage.getUserId())
        assertEquals("alex.morgan@example.com", tokenStorage.getUserEmail())

        val recordedRequest = server.takeRequest()
        assertEquals("/api/v1/auth/login", recordedRequest.path)
    }

    @Test
    fun testLoginFailureReturnsErrorWithoutSavingTokens() = runBlocking {
        val json = """
            {
                "success": false,
                "error": "Unauthorized",
                "message": "Invalid email or password",
                "statusCode": 401,
                "timestamp": "2026-08-29T15:00:00.000Z"
            }
        """.trimIndent()

        server.enqueue(MockResponse().setResponseCode(401).setBody(json))

        val result = authRepository.login("alex.morgan@example.com", "WrongPassword")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Invalid email or password") == true)

        assertNull(tokenStorage.getAccessToken())
        assertNull(tokenStorage.getRefreshToken())
    }

    @Test
    fun testRegisterSuccessSavesTokensAndUser() = runBlocking {
        val json = """
            {
                "success": true,
                "message": "Account registered successfully",
                "data": {
                    "user": {
                        "id": "usr-99999",
                        "email": "new.user@example.com",
                        "role": "USER"
                    },
                    "tokens": {
                        "accessToken": "new-access-token",
                        "refreshToken": "new-refresh-token"
                    }
                },
                "timestamp": "2026-08-29T15:00:00.000Z"
            }
        """.trimIndent()

        server.enqueue(MockResponse().setResponseCode(201).setBody(json))

        val result = authRepository.register("new.user@example.com", "SecurePassword123!")
        assertTrue(result.isSuccess)

        assertEquals("new-access-token", tokenStorage.getAccessToken())
        assertEquals("new-refresh-token", tokenStorage.getRefreshToken())
        assertEquals("usr-99999", tokenStorage.getUserId())

        val recordedRequest = server.takeRequest()
        assertEquals("/api/v1/auth/register", recordedRequest.path)
    }

    @Test
    fun testRegisterFailureConflictReturnsErrorMessage() = runBlocking {
        val json = """
            {
                "success": false,
                "error": "Conflict",
                "message": "An account with this email address already exists",
                "statusCode": 409,
                "timestamp": "2026-08-29T15:00:00.000Z"
            }
        """.trimIndent()

        server.enqueue(MockResponse().setResponseCode(409).setBody(json))

        val result = authRepository.register("existing@example.com", "Password123!")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("already exists") == true)

        assertNull(tokenStorage.getAccessToken())
    }

    @Test
    fun testRefreshTokenSuccessUpdatesStoredTokens() = runBlocking {
        tokenStorage.saveTokens("old-access-token", "old-refresh-token")

        val json = """
            {
                "success": true,
                "message": "Token refreshed successfully",
                "data": {
                    "tokens": {
                        "accessToken": "rotated-access-token",
                        "refreshToken": "rotated-refresh-token",
                        "expiresIn": "15m"
                    }
                },
                "timestamp": "2026-08-29T15:00:00.000Z"
            }
        """.trimIndent()

        server.enqueue(MockResponse().setResponseCode(200).setBody(json))

        val result = authRepository.refreshToken()
        assertTrue(result.isSuccess)
        assertEquals("rotated-access-token", tokenStorage.getAccessToken())
        assertEquals("rotated-refresh-token", tokenStorage.getRefreshToken())

        val recordedRequest = server.takeRequest()
        assertEquals("/api/v1/auth/refresh", recordedRequest.path)
        assertTrue(recordedRequest.body.readUtf8().contains("old-refresh-token"))
    }

    @Test
    fun testRefreshTokenFailureOn401ClearsLocalSession() = runBlocking {
        tokenStorage.saveTokens("old-access-token", "revoked-refresh-token")

        val json = """
            {
                "success": false,
                "error": "Unauthorized",
                "message": "Invalid or revoked refresh token",
                "statusCode": 401
            }
        """.trimIndent()

        server.enqueue(MockResponse().setResponseCode(401).setBody(json))

        val result = authRepository.refreshToken()
        assertTrue(result.isFailure)

        // Stored credentials must be cleared on 401
        assertNull(tokenStorage.getAccessToken())
        assertNull(tokenStorage.getRefreshToken())
    }

    @Test
    fun testLogoutCallsApiAndClearsLocalTokens() = runBlocking {
        tokenStorage.saveTokens("valid-access-token", "valid-refresh-token")

        val json = """
            {
                "success": true,
                "message": "Logged out successfully"
            }
        """.trimIndent()

        server.enqueue(MockResponse().setResponseCode(200).setBody(json))

        val result = authRepository.logout()
        assertTrue(result.isSuccess)

        assertNull(tokenStorage.getAccessToken())
        assertNull(tokenStorage.getRefreshToken())

        val recordedRequest = server.takeRequest()
        assertEquals("/api/v1/auth/logout", recordedRequest.path)
        assertTrue(recordedRequest.body.readUtf8().contains("valid-refresh-token"))
    }

    @Test
    fun testLogoutWhenApiThrowsStillClearsLocalTokens() = runBlocking {
        tokenStorage.saveTokens("valid-access-token", "valid-refresh-token")

        server.enqueue(MockResponse().setResponseCode(500).setBody("Internal Error"))

        val result = authRepository.logout()
        assertTrue(result.isSuccess)

        // Stored credentials must be cleared even when API fails
        assertNull(tokenStorage.getAccessToken())
        assertNull(tokenStorage.getRefreshToken())
    }

    @Test
    fun testHasActiveSession() = runBlocking {
        assertFalse(authRepository.hasActiveSession())

        tokenStorage.saveTokens("token-xyz", "refresh-xyz")
        assertTrue(authRepository.hasActiveSession())

        tokenStorage.clear()
        assertFalse(authRepository.hasActiveSession())
    }

    @Test
    fun testCheckSessionValid() = runBlocking {
        tokenStorage.saveTokens("valid-token", "valid-refresh")

        val json = """
            {
                "success": true,
                "data": {
                    "user": {
                        "id": "usr-111",
                        "email": "alex.morgan@example.com",
                        "role": "USER"
                    }
                }
            }
        """.trimIndent()

        server.enqueue(MockResponse().setResponseCode(200).setBody(json))

        val result = authRepository.checkSession()
        assertTrue(result.isSuccess)
        assertEquals("usr-111", result.getOrNull()?.id)
        assertEquals("alex.morgan@example.com", result.getOrNull()?.email)
    }
}
