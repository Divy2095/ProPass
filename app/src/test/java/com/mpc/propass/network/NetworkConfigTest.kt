package com.mpc.propass.network

import com.mpc.propass.network.config.NetworkConfig
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class NetworkConfigTest {

    @Before
    @After
    fun resetConfig() {
        NetworkConfig.resetBaseUrl()
    }

    @Test
    fun testDefaultBaseUrl() {
        assertEquals("http://10.0.2.2:3000/", NetworkConfig.baseUrl)
        assertEquals("http://10.0.2.2:3000/", NetworkConfig.DEFAULT_BASE_URL)
        assertEquals("http://127.0.0.1:3000/", NetworkConfig.DEVICE_ADB_REVERSE_BASE_URL)
    }

    @Test
    fun testTrailingSlashNormalization() {
        NetworkConfig.baseUrl = "http://192.168.1.50:3000"
        assertEquals("http://192.168.1.50:3000/", NetworkConfig.baseUrl)

        NetworkConfig.baseUrl = "http://192.168.1.50:3000/"
        assertEquals("http://192.168.1.50:3000/", NetworkConfig.baseUrl)
    }

    @Test
    fun testResetBaseUrl() {
        NetworkConfig.baseUrl = "http://custom-host:8080"
        assertEquals("http://custom-host:8080/", NetworkConfig.baseUrl)

        NetworkConfig.resetBaseUrl()
        assertEquals(NetworkConfig.DEFAULT_BASE_URL, NetworkConfig.baseUrl)
    }

    @Test
    fun testTimeoutsAreReasonable() {
        assertTrue(NetworkConfig.CONNECT_TIMEOUT_SECONDS >= 10L)
        assertTrue(NetworkConfig.READ_TIMEOUT_SECONDS >= 10L)
        assertTrue(NetworkConfig.WRITE_TIMEOUT_SECONDS >= 10L)
    }
}
