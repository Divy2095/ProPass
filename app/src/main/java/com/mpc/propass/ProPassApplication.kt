package com.mpc.propass

import android.app.Application
import com.mpc.propass.data.local.DataStoreTokenStorage
import com.mpc.propass.data.local.TokenStorage
import com.mpc.propass.data.repository.AuthRepository
import com.mpc.propass.data.repository.AuthRepositoryImpl
import com.mpc.propass.network.NetworkClient
import com.mpc.propass.network.interceptor.DataStoreTokenProvider

/**
 * Base Application class for ProPass Android application.
 *
 * Initializes persistent token storage, configures the network client's token provider,
 * and sets up the authentication repository.
 */
class ProPassApplication : Application() {

    lateinit var tokenStorage: TokenStorage
        private set

    lateinit var authRepository: AuthRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        // 1. Initialize persistent DataStore token storage
        tokenStorage = DataStoreTokenStorage.create(this)

        // 2. Wire DataStore token provider to Phase 3A NetworkClient
        val tokenProvider = DataStoreTokenProvider(tokenStorage)
        NetworkClient.setTokenProvider(tokenProvider)

        // 3. Initialize authentication repository
        authRepository = AuthRepositoryImpl(
            apiService = NetworkClient.apiService,
            tokenStorage = tokenStorage
        )
    }

    companion object {
        lateinit var instance: ProPassApplication
            private set
    }
}
