package com.mpc.propass.data.local

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TokenStorageTest {

    private lateinit var tokenStorage: FakeTokenStorage

    @Before
    fun setUp() {
        tokenStorage = FakeTokenStorage()
    }

    @Test
    fun testInitialStateIsEmpty() = runBlocking {
        assertNull(tokenStorage.getAccessToken())
        assertNull(tokenStorage.getRefreshToken())
        assertNull(tokenStorage.getUserId())
        assertNull(tokenStorage.getUserEmail())
        assertNull(tokenStorage.getUserRole())
        assertFalse(tokenStorage.isAuthenticatedFlow.first())
    }

    @Test
    fun testSaveTokensUpdatesCacheAndFlows() = runBlocking {
        tokenStorage.saveTokens("access-123", "refresh-456")

        assertEquals("access-123", tokenStorage.getAccessToken())
        assertEquals("refresh-456", tokenStorage.getRefreshToken())
        assertEquals("access-123", tokenStorage.accessTokenFlow.first())
        assertEquals("refresh-456", tokenStorage.refreshTokenFlow.first())
        assertTrue(tokenStorage.isAuthenticatedFlow.first())
    }

    @Test
    fun testSaveUserUpdatesCacheAndRole() = runBlocking {
        tokenStorage.saveUser("user-id-001", "alex.morgan@example.com", "ORGANIZER")

        assertEquals("user-id-001", tokenStorage.getUserId())
        assertEquals("alex.morgan@example.com", tokenStorage.getUserEmail())
        assertEquals("ORGANIZER", tokenStorage.getUserRole())
        assertEquals("ORGANIZER", tokenStorage.userRoleFlow.first())
    }

    @Test
    fun testClearRemovesAllCredentials() = runBlocking {
        tokenStorage.saveTokens("access-123", "refresh-456")
        tokenStorage.saveUser("user-id-001", "alex.morgan@example.com", "ORGANIZER")

        tokenStorage.clear()

        assertNull(tokenStorage.getAccessToken())
        assertNull(tokenStorage.getRefreshToken())
        assertNull(tokenStorage.getUserId())
        assertNull(tokenStorage.getUserEmail())
        assertNull(tokenStorage.getUserRole())
        assertFalse(tokenStorage.isAuthenticatedFlow.first())
    }
}
