package com.mpc.propass

import android.app.Application
import com.mpc.propass.data.local.DataStoreTokenStorage
import com.mpc.propass.data.local.TokenStorage
import com.mpc.propass.data.repository.AuthRepository
import com.mpc.propass.data.repository.AuthRepositoryImpl
import com.mpc.propass.data.repository.DashboardRepository
import com.mpc.propass.data.repository.DashboardRepositoryImpl
import com.mpc.propass.data.repository.UserRepository
import com.mpc.propass.data.repository.UserRepositoryImpl
import com.mpc.propass.network.NetworkClient
import com.mpc.propass.network.interceptor.DataStoreTokenProvider

/**
 * Base Application class for ProPass Android application.
 *
 * Initializes persistent token storage, configures the network client's token provider,
 * and sets up the authentication, user, and dashboard repositories.
 */
class ProPassApplication : Application() {

    lateinit var tokenStorage: TokenStorage
        private set

    lateinit var authRepository: AuthRepository
        private set

    lateinit var userRepository: UserRepository
        private set

    lateinit var dashboardRepository: DashboardRepository
        private set

    lateinit var eventRepository: com.mpc.propass.data.repository.EventRepository
        private set

    lateinit var registrationRepository: com.mpc.propass.data.repository.RegistrationRepository
        private set

    lateinit var digitalPassRepository: com.mpc.propass.data.repository.DigitalPassRepository
        private set

    lateinit var organizerEventRepository: com.mpc.propass.organizer.data.OrganizerEventRepository
        private set

    lateinit var organizerFormRepository: com.mpc.propass.organizer.data.OrganizerFormRepository
        private set

    lateinit var organizerRegistrationRepository: com.mpc.propass.organizer.data.OrganizerRegistrationRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        // 0. Auto-configure base URL: use adb reverse loopback on physical device
        if (!isEmulator()) {
            com.mpc.propass.network.config.NetworkConfig.baseUrl =
                com.mpc.propass.network.config.NetworkConfig.DEVICE_ADB_REVERSE_BASE_URL
            NetworkClient.reset()
        }

        // 1. Initialize persistent DataStore token storage
        tokenStorage = DataStoreTokenStorage.create(this)

        // 2. Wire DataStore token provider and token storage to NetworkClient
        val tokenProvider = DataStoreTokenProvider(tokenStorage)
        NetworkClient.setTokenProvider(tokenProvider)
        NetworkClient.setTokenStorage(tokenStorage)

        // 3. Initialize repositories
        authRepository = AuthRepositoryImpl(
            apiService = NetworkClient.apiService,
            tokenStorage = tokenStorage
        )
        userRepository = UserRepositoryImpl(
            apiService = NetworkClient.apiService
        )
        dashboardRepository = DashboardRepositoryImpl(
            apiService = NetworkClient.apiService
        )
        eventRepository = com.mpc.propass.data.repository.EventRepositoryImpl(
            apiService = NetworkClient.apiService
        )
        registrationRepository = com.mpc.propass.data.repository.RegistrationRepositoryImpl(
            apiService = NetworkClient.apiService
        )
        digitalPassRepository = com.mpc.propass.data.repository.DigitalPassRepositoryImpl(
            apiService = NetworkClient.apiService
        )
        organizerEventRepository = com.mpc.propass.organizer.data.OrganizerEventRepositoryImpl(
            apiService = NetworkClient.apiService
        )
        organizerFormRepository = com.mpc.propass.organizer.data.OrganizerFormRepositoryImpl(
            apiService = NetworkClient.apiService
        )
        organizerRegistrationRepository = com.mpc.propass.organizer.data.OrganizerRegistrationRepositoryImpl(
            apiService = NetworkClient.apiService
        )
    }

    private fun isEmulator(): Boolean {
        return (android.os.Build.BRAND.startsWith("generic") && android.os.Build.DEVICE.startsWith("generic"))
            || android.os.Build.FINGERPRINT.startsWith("generic")
            || android.os.Build.FINGERPRINT.startsWith("unknown")
            || android.os.Build.HARDWARE.contains("goldfish")
            || android.os.Build.HARDWARE.contains("ranchu")
            || android.os.Build.MODEL.contains("google_sdk")
            || android.os.Build.MODEL.contains("Emulator")
            || android.os.Build.MODEL.contains("Android SDK built for x86")
            || android.os.Build.MANUFACTURER.contains("Genymotion")
            || android.os.Build.PRODUCT.contains("sdk_google")
            || android.os.Build.PRODUCT.contains("google_sdk")
            || android.os.Build.PRODUCT.contains("sdk")
            || android.os.Build.PRODUCT.contains("sdk_x86")
            || android.os.Build.PRODUCT.contains("vbox86p")
            || android.os.Build.PRODUCT.contains("emulator")
            || android.os.Build.PRODUCT.contains("simulator")
    }

    companion object {
        lateinit var instance: ProPassApplication
            private set
    }
}
