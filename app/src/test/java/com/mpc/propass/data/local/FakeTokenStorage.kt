package com.mpc.propass.data.local

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

/**
 * In-memory test double for [TokenStorage] enabling fast, isolated unit testing.
 */
class FakeTokenStorage : TokenStorage {

    private val _accessToken = MutableStateFlow<String?>(null)
    private val _refreshToken = MutableStateFlow<String?>(null)
    private val _userId = MutableStateFlow<String?>(null)
    private val _userEmail = MutableStateFlow<String?>(null)
    private val _userRole = MutableStateFlow<String?>(null)

    override suspend fun saveTokens(accessToken: String, refreshToken: String) {
        _accessToken.value = accessToken
        _refreshToken.value = refreshToken
    }

    override suspend fun saveUser(userId: String, email: String, role: String?) {
        _userId.value = userId
        _userEmail.value = email
        _userRole.value = role
    }

    override suspend fun clear() {
        _accessToken.value = null
        _refreshToken.value = null
        _userId.value = null
        _userEmail.value = null
        _userRole.value = null
    }

    override fun getAccessToken(): String? = _accessToken.value
    override fun getRefreshToken(): String? = _refreshToken.value
    override fun getUserId(): String? = _userId.value
    override fun getUserEmail(): String? = _userEmail.value
    override fun getUserRole(): String? = _userRole.value

    override val accessTokenFlow: Flow<String?> = _accessToken.asStateFlow()
    override val refreshTokenFlow: Flow<String?> = _refreshToken.asStateFlow()
    override val userRoleFlow: Flow<String?> = _userRole.asStateFlow()
    override val isAuthenticatedFlow: Flow<Boolean> = _accessToken.map { !it.isNullOrBlank() }
}
