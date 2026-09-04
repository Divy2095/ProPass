package com.mpc.propass.network.interceptor

import com.mpc.propass.data.local.TokenStorage

/**
 * Bridges the persistent [TokenStorage] layer to the Phase 3A [TokenProvider] interface,
 * allowing [AuthInterceptor] to transparently attach stored access tokens to requests.
 */
class DataStoreTokenProvider(
    private val tokenStorage: TokenStorage
) : TokenProvider {

    override fun getAccessToken(): String? {
        return tokenStorage.getAccessToken()
    }
}
