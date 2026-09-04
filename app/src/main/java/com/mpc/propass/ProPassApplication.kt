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

    override fun onCreate() {
        super.onCreate()
        instance = this

        // 1. Initialize persistent DataStore token storage
        tokenStorage = DataStoreTokenStorage.create(this)

        // 2. Wire DataStore token provider to Phase 3A NetworkClient
        val tokenProvider = DataStoreTokenProvider(tokenStorage)
        NetworkClient.setTokenProvider(tokenProvider)

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
    }

    companion object {
        lateinit var instance: ProPassApplication
            private set
    }
}
