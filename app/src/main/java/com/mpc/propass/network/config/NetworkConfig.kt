package com.mpc.propass.network.config

/**
 * Centralized network configuration for ProPass Android application.
 */
object NetworkConfig {

    /**
     * Production backend hosted on Render.
     */
    const val PRODUCTION_BASE_URL = "https://propass-api.onrender.com/"

    /**
     * Android Studio Emulator → local backend.
     */
    const val EMULATOR_BASE_URL = "http://10.0.2.2:3000/"

    /**
     * Physical device → local backend through ADB reverse.
     */
    const val DEVICE_ADB_REVERSE_BASE_URL = "http://127.0.0.1:3000/"

    /**
     * Physical device → local backend over Wi-Fi.
     */
    const val DEVICE_WIFI_BASE_URL = "http://10.235.123.153:3000/"

    /**
     * Production is the default.
     */
    const val DEFAULT_BASE_URL = PRODUCTION_BASE_URL

    const val CONNECT_TIMEOUT_SECONDS = 15L
    const val READ_TIMEOUT_SECONDS = 20L
    const val WRITE_TIMEOUT_SECONDS = 20L

    @Volatile
    var baseUrl: String = DEFAULT_BASE_URL
        set(value) {
            field = if (value.endsWith("/")) value else "$value/"
        }

    fun resetBaseUrl() {
        baseUrl = DEFAULT_BASE_URL
    }
}