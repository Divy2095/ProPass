package com.mpc.propass.network

import com.mpc.propass.data.local.TokenStorage
import com.mpc.propass.network.api.ProPassApiService
import com.mpc.propass.network.config.NetworkConfig
import com.mpc.propass.network.interceptor.AuthInterceptor
import com.mpc.propass.network.interceptor.NoOpTokenProvider
import com.mpc.propass.network.interceptor.TokenAuthenticator
import com.mpc.propass.network.interceptor.TokenProvider
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Singleton networking client and factory for the ProPass Android client.
 *
 * Configures Moshi JSON serialization, OkHttp transport with authentication,
 * token refreshment, and logging interceptors, and Retrofit 2 REST client.
 */
object NetworkClient {

    @Volatile
    private var currentTokenProvider: TokenProvider = NoOpTokenProvider

    @Volatile
    private var currentTokenStorage: TokenStorage? = null

    @Volatile
    private var cachedApiService: ProPassApiService? = null

    /**
     * Shared Moshi instance configured with Kotlin reflection adapter factory.
     */
    val moshi: Moshi by lazy {
        Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    /**
     * Builds an OkHttpClient configured with authentication and logging.
     *
     * @param tokenProvider Provides access tokens for outgoing requests.
     * @param tokenStorage Provides token persistence for transparent 401 refresh.
     * @param enableLogging Whether to enable HTTP body logging (defaults to true in debug builds).
     */
    fun createOkHttpClient(
        tokenProvider: TokenProvider = currentTokenProvider,
        tokenStorage: TokenStorage? = currentTokenStorage,
        enableLogging: Boolean = true
    ): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(NetworkConfig.CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(NetworkConfig.READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(NetworkConfig.WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .addInterceptor(AuthInterceptor(tokenProvider))

        if (tokenStorage != null) {
            builder.authenticator(TokenAuthenticator(tokenStorage))
        }

        if (enableLogging) {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            builder.addInterceptor(loggingInterceptor)
        }

        return builder.build()
    }

    /**
     * Builds a Retrofit instance configured with the specified base URL and OkHttpClient.
     *
     * @param baseUrl Target base URL (defaults to [NetworkConfig.baseUrl]).
     * @param okHttpClient Custom OkHttpClient if desired.
     */
    fun createRetrofit(
        baseUrl: String = NetworkConfig.baseUrl,
        okHttpClient: OkHttpClient = createOkHttpClient()
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    /**
     * Factory function creating a new [ProPassApiService] instance with custom parameters.
     */
    fun createApiService(
        baseUrl: String = NetworkConfig.baseUrl,
        tokenProvider: TokenProvider = currentTokenProvider,
        tokenStorage: TokenStorage? = currentTokenStorage,
        enableLogging: Boolean = true
    ): ProPassApiService {
        val client = createOkHttpClient(tokenProvider, tokenStorage, enableLogging)
        val retrofit = createRetrofit(baseUrl, client)
        return retrofit.create(ProPassApiService::class.java)
    }

    /**
     * Updates the active token provider and invalidates any cached service instances.
     * Ready for Phase 3B DataStore integration.
     */
    @Synchronized
    fun setTokenProvider(tokenProvider: TokenProvider) {
        currentTokenProvider = tokenProvider
        cachedApiService = null
    }

    /**
     * Updates the active token storage and invalidates any cached service instances.
     */
    @Synchronized
    fun setTokenStorage(tokenStorage: TokenStorage?) {
        currentTokenStorage = tokenStorage
        cachedApiService = null
    }

    /**
     * Retrieves the current token provider.
     */
    fun getTokenProvider(): TokenProvider = currentTokenProvider

    /**
     * Retrieves the current token storage.
     */
    fun getTokenStorage(): TokenStorage? = currentTokenStorage

    /**
     * Default shared instance of [ProPassApiService] using active configuration.
     */
    val apiService: ProPassApiService
        get() {
            return cachedApiService ?: synchronized(this) {
                cachedApiService ?: createApiService().also { cachedApiService = it }
            }
        }

    /**
     * Clears cached service instances (e.g. after base URL change or during tests).
     */
    @Synchronized
    fun reset() {
        cachedApiService = null
    }
}
