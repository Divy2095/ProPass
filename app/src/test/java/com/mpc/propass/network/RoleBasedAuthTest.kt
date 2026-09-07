package com.mpc.propass.network

import com.mpc.propass.data.local.FakeTokenStorage
import com.mpc.propass.data.repository.AuthRepository
import com.mpc.propass.data.repository.AuthRepositoryImpl
import com.mpc.propass.network.model.AuthUserDto
import com.mpc.propass.network.model.UserRole
import kotlinx.coroutines.flow.first
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

class RoleBasedAuthTest {

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
    fun testUserRoleEnumMapping() {
        assertEquals(UserRole.ORGANIZER, UserRole.fromString("ORGANIZER"))
        assertEquals(UserRole.ORGANIZER, UserRole.fromString("organizer"))
        assertEquals(UserRole.ATTENDEE, UserRole.fromString("ATTENDEE"))
        assertEquals(UserRole.ATTENDEE, UserRole.fromString("attendee"))
        assertEquals(UserRole.ATTENDEE, UserRole.fromString(null))
        assertEquals(UserRole.ATTENDEE, UserRole.fromString(""))
        assertEquals(UserRole.ATTENDEE, UserRole.fromString("UNKNOWN_ROLE"))
    }

    @Test
    fun testAuthUserDtoRoleParsing() {
        val moshi = NetworkClient.moshi
        val adapter = moshi.adapter(AuthUserDto::class.java)

        // Organizer JSON
        val organizerJson = """
            {
                "id": "org-001",
                "email": "organizer@propass.id",
                "role": "ORGANIZER"
            }
        """.trimIndent()
        val organizerUser = adapter.fromJson(organizerJson)
        assertEquals("ORGANIZER", organizerUser?.role)
        assertTrue(organizerUser?.isOrganizer == true)
        assertEquals(UserRole.ORGANIZER, organizerUser?.userRole)

        // Attendee JSON
        val attendeeJson = """
            {
                "id": "att-001",
                "email": "attendee@propass.id",
                "role": "ATTENDEE"
            }
        """.trimIndent()
        val attendeeUser = adapter.fromJson(attendeeJson)
        assertEquals("ATTENDEE", attendeeUser?.role)
        assertFalse(attendeeUser?.isOrganizer == true)
        assertEquals(UserRole.ATTENDEE, attendeeUser?.userRole)

        // Missing role JSON defaults to ATTENDEE
        val defaultJson = """
            {
                "id": "def-001",
                "email": "default@propass.id"
            }
        """.trimIndent()
        val defaultUser = adapter.fromJson(defaultJson)
        assertEquals("ATTENDEE", defaultUser?.role)
        assertFalse(defaultUser?.isOrganizer == true)
        assertEquals(UserRole.ATTENDEE, defaultUser?.userRole)
    }

    @Test
    fun testOrganizerLoginPersistsRoleAndExposesOrganizerStatus() = runBlocking {
        val json = """
            {
                "success": true,
                "message": "Login successful",
                "data": {
                    "user": {
                        "id": "org-101",
                        "email": "organizer@propass.id",
                        "role": "ORGANIZER",
                        "createdAt": "2026-09-07T12:00:00.000Z"
                    },
                    "tokens": {
                        "accessToken": "organizer-jwt-token",
                        "refreshToken": "organizer-refresh-token",
                        "expiresIn": "15m"
                    }
                },
                "timestamp": "2026-09-07T12:00:00.000Z"
            }
        """.trimIndent()

        server.enqueue(MockResponse().setResponseCode(200).setBody(json))

        val result = authRepository.login("organizer@propass.id", "Organizer123!")
        assertTrue(result.isSuccess)

        // Check role in TokenStorage and AuthRepository
        assertEquals("ORGANIZER", tokenStorage.getUserRole())
        assertEquals("ORGANIZER", authRepository.getUserRole())
        assertTrue(authRepository.isOrganizer())
        assertEquals("ORGANIZER", authRepository.userRoleFlow.first())
    }

    @Test
    fun testAttendeeLoginPersistsAttendeeRole() = runBlocking {
        val json = """
            {
                "success": true,
                "message": "Login successful",
                "data": {
                    "user": {
                        "id": "att-202",
                        "email": "attendee@propass.id",
                        "role": "ATTENDEE",
                        "createdAt": "2026-09-07T12:00:00.000Z"
                    },
                    "tokens": {
                        "accessToken": "attendee-jwt-token",
                        "refreshToken": "attendee-refresh-token",
                        "expiresIn": "15m"
                    }
                },
                "timestamp": "2026-09-07T12:00:00.000Z"
            }
        """.trimIndent()

        server.enqueue(MockResponse().setResponseCode(200).setBody(json))

        val result = authRepository.login("attendee@propass.id", "Attendee123!")
        assertTrue(result.isSuccess)

        assertEquals("ATTENDEE", tokenStorage.getUserRole())
        assertEquals("ATTENDEE", authRepository.getUserRole())
        assertFalse(authRepository.isOrganizer())
    }

    @Test
    fun testLogoutClearsUserRole() = runBlocking {
        tokenStorage.saveTokens("token-123", "refresh-123")
        tokenStorage.saveUser("org-101", "organizer@propass.id", "ORGANIZER")
        assertTrue(authRepository.isOrganizer())

        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"success": true}"""))

        val logoutResult = authRepository.logout()
        assertTrue(logoutResult.isSuccess)

        assertNull(tokenStorage.getUserRole())
        assertNull(authRepository.getUserRole())
        assertFalse(authRepository.isOrganizer())
    }
}
