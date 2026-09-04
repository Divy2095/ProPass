package com.mpc.propass.network.interceptor

/**
 * Interface contract for supplying access tokens to outgoing network requests.
 *
 * This abstraction separates the network transport layer from the token persistence mechanism.
 * - Phase 3A: Uses InMemoryTokenProvider or NoOpTokenProvider.
 * - Phase 3B: Will be implemented by Encrypted DataStore token repository.
 */
interface TokenProvider {

    /**
     * Retrieves the current access token, or null if unauthenticated.
     */
    fun getAccessToken(): String?
}

/**
 * No-operation token provider for unauthenticated or initial setup scenarios.
 */
object NoOpTokenProvider : TokenProvider {
    override fun getAccessToken(): String? = null
}

/**
 * Simple thread-safe in-memory token provider suitable for testing and development.
 */
class InMemoryTokenProvider(
    @Volatile private var token: String? = null
) : TokenProvider {

    override fun getAccessToken(): String? = token

    fun setAccessToken(newToken: String?) {
        token = newToken
    }

    fun clearToken() {
        token = null
    }
}
