package com.mpc.propass.network.interceptor

import com.mpc.propass.data.local.FakeTokenStorage
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class DataStoreTokenProviderTest {

    private lateinit var tokenStorage: FakeTokenStorage
    private lateinit var tokenProvider: DataStoreTokenProvider

    @Before
    fun setUp() {
        tokenStorage = FakeTokenStorage()
        tokenProvider = DataStoreTokenProvider(tokenStorage)
    }

    @Test
    fun testGetAccessTokenReturnsNullInitially() {
        assertNull(tokenProvider.getAccessToken())
    }

    @Test
    fun testGetAccessTokenReturnsSavedToken() = runBlocking {
        tokenStorage.saveTokens("jwt-sample-access-token", "jwt-sample-refresh-token")
        assertEquals("jwt-sample-access-token", tokenProvider.getAccessToken())
    }

    @Test
    fun testGetAccessTokenReturnsNullAfterClear() = runBlocking {
        tokenStorage.saveTokens("jwt-sample-access-token", "jwt-sample-refresh-token")
        assertEquals("jwt-sample-access-token", tokenProvider.getAccessToken())

        tokenStorage.clear()
        assertNull(tokenProvider.getAccessToken())
    }
}
