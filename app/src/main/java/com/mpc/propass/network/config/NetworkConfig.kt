package com.mpc.propass.network.config

/**
 * Centralized network configuration for ProPass Android application.
 *
 * Provides base URLs for various development environments:
 * - Android Studio Emulator: http://10.0.2.2:3000/ (maps to localhost on host machine)
 * - Physical Device with ADB reverse: http://127.0.0.1:3000/ (run `adb reverse tcp:3000 tcp:3000`)
 * - Physical Device over Local Wi-Fi: http://<HOST_LAN_IP>:3000/
 */
object NetworkConfig {

    /**
     * Standard loopback address for the Android Studio Emulator pointing to host PC.
     */
    const val EMULATOR_BASE_URL = "http://10.0.2.2:3000/"

    /**
     * Loopback address for physical device when using `adb reverse tcp:3000 tcp:3000`.
     */
    const val DEVICE_ADB_REVERSE_BASE_URL = "http://127.0.0.1:3000/"

    /**
     * Default base URL used across the application.
     */
    const val DEFAULT_BASE_URL = EMULATOR_BASE_URL

    /**
     * Network timeouts in seconds.
     */
    const val CONNECT_TIMEOUT_SECONDS = 15L
    const val READ_TIMEOUT_SECONDS = 20L
    const val WRITE_TIMEOUT_SECONDS = 20L

    /**
     * Active base URL. Can be modified at runtime (e.g. for testing, switching between
     * emulator and physical hardware, or configuring LAN IP).
     */
    @Volatile
    var baseUrl: String = DEFAULT_BASE_URL
        set(value) {
            field = if (value.endsWith("/")) value else "$value/"
        }

    /**
     * Resets the base URL back to default.
     */
    fun resetBaseUrl() {
        baseUrl = DEFAULT_BASE_URL
    }
}
