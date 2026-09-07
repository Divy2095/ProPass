package com.mpc.propass.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.IOException

private val Context.authDataStore: DataStore<Preferences> by preferencesDataStore(name = "propass_auth_prefs")

/**
 * Storage contract for persistent authentication tokens and user credentials.
 */
interface TokenStorage {
    suspend fun saveTokens(accessToken: String, refreshToken: String)
    suspend fun saveUser(userId: String, email: String, role: String? = null)
    suspend fun clear()

    fun getAccessToken(): String?
    fun getRefreshToken(): String?
    fun getUserId(): String?
    fun getUserEmail(): String?
    fun getUserRole(): String?

    val accessTokenFlow: Flow<String?>
    val refreshTokenFlow: Flow<String?>
    val userRoleFlow: Flow<String?>
    val isAuthenticatedFlow: Flow<Boolean>
}

/**
 * Production implementation of [TokenStorage] backed by AndroidX Preferences DataStore.
 *
 * Maintains an in-memory volatile cache of the access and refresh tokens to ensure
 * instantaneous, thread-safe, non-blocking synchronous reads for OkHttp interceptors.
 */
class DataStoreTokenStorage(
    private val dataStore: DataStore<Preferences>,
    scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) : TokenStorage {

    companion object {
        val KEY_ACCESS_TOKEN = stringPreferencesKey("propass_access_token")
        val KEY_REFRESH_TOKEN = stringPreferencesKey("propass_refresh_token")
        val KEY_USER_ID = stringPreferencesKey("propass_user_id")
        val KEY_USER_EMAIL = stringPreferencesKey("propass_user_email")
        val KEY_USER_ROLE = stringPreferencesKey("propass_user_role")

        /**
         * Convenience factory creating an instance bound to Context's DataStore.
         */
        fun create(context: Context): DataStoreTokenStorage {
            return DataStoreTokenStorage(context.applicationContext.authDataStore)
        }
    }

    @Volatile
    private var cachedAccessToken: String? = null

    @Volatile
    private var cachedRefreshToken: String? = null

    @Volatile
    private var cachedUserId: String? = null

    @Volatile
    private var cachedUserEmail: String? = null

    @Volatile
    private var cachedUserRole: String? = null

    init {
        // Eagerly observe DataStore changes to keep in-memory cache synchronized
        scope.launch {
            dataStore.data
                .catch { exception ->
                    if (exception is IOException) {
                        emit(emptyPreferences())
                    } else {
                        throw exception
                    }
                }
                .collect { preferences ->
                    cachedAccessToken = preferences[KEY_ACCESS_TOKEN]
                    cachedRefreshToken = preferences[KEY_REFRESH_TOKEN]
                    cachedUserId = preferences[KEY_USER_ID]
                    cachedUserEmail = preferences[KEY_USER_EMAIL]
                    cachedUserRole = preferences[KEY_USER_ROLE]
                }
        }
    }

    override suspend fun saveTokens(accessToken: String, refreshToken: String) {
        cachedAccessToken = accessToken
        cachedRefreshToken = refreshToken
        dataStore.edit { preferences ->
            preferences[KEY_ACCESS_TOKEN] = accessToken
            preferences[KEY_REFRESH_TOKEN] = refreshToken
        }
    }

    override suspend fun saveUser(userId: String, email: String, role: String?) {
        cachedUserId = userId
        cachedUserEmail = email
        cachedUserRole = role
        dataStore.edit { preferences ->
            preferences[KEY_USER_ID] = userId
            preferences[KEY_USER_EMAIL] = email
            if (role != null) {
                preferences[KEY_USER_ROLE] = role
            } else {
                preferences.remove(KEY_USER_ROLE)
            }
        }
    }

    override suspend fun clear() {
        cachedAccessToken = null
        cachedRefreshToken = null
        cachedUserId = null
        cachedUserEmail = null
        cachedUserRole = null
        dataStore.edit { preferences ->
            preferences.remove(KEY_ACCESS_TOKEN)
            preferences.remove(KEY_REFRESH_TOKEN)
            preferences.remove(KEY_USER_ID)
            preferences.remove(KEY_USER_EMAIL)
            preferences.remove(KEY_USER_ROLE)
        }
    }

    override fun getAccessToken(): String? = cachedAccessToken

    override fun getRefreshToken(): String? = cachedRefreshToken

    override fun getUserId(): String? = cachedUserId

    override fun getUserEmail(): String? = cachedUserEmail

    override fun getUserRole(): String? = cachedUserRole

    override val accessTokenFlow: Flow<String?> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_ACCESS_TOKEN] }

    override val refreshTokenFlow: Flow<String?> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_REFRESH_TOKEN] }

    override val userRoleFlow: Flow<String?> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_USER_ROLE] }

    override val isAuthenticatedFlow: Flow<Boolean> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { !it[KEY_ACCESS_TOKEN].isNullOrBlank() }
}
