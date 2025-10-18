package com.kevdadev.musicminds.auth.api

import android.util.Base64
import android.util.Log
import com.kevdadev.musicminds.auth.data.AuthConstants
import com.kevdadev.musicminds.auth.data.AuthResult
import com.kevdadev.musicminds.auth.data.AuthTokens
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Client for Spotify Web API token operations
 */
class SpotifyWebApiClient {
    
    companion object {
        private const val TAG = "SpotifyWebApiClient"
        private const val BASE_URL = "https://accounts.spotify.com/"
        // TODO: Add your CLIENT_SECRET from Spotify Developer Dashboard here
        // SECURITY NOTE: In production, store this securely (not in source code)
        // For now, you need to add your client secret from https://developer.spotify.com/dashboard
        private const val CLIENT_SECRET = "22737950ab49475f8d31e8cb98b51662" // REPLACE WITH YOUR CLIENT SECRET
        
        @Volatile
        private var INSTANCE: SpotifyWebApiClient? = null
        
        fun getInstance(): SpotifyWebApiClient {
            return INSTANCE ?: synchronized(this) {
                val instance = SpotifyWebApiClient()
                INSTANCE = instance
                instance
            }
        }
    }
    
    private val apiService: SpotifyApiService
    
    init {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        val httpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
        
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        
        apiService = retrofit.create(SpotifyApiService::class.java)
    }
    
    /**
     * Exchange authorization code for access and refresh tokens
     */
    suspend fun exchangeCodeForTokens(authCode: String): AuthResult<AuthTokens> {
        return try {
            Log.d(TAG, "Exchanging authorization code for tokens")
            
            if (CLIENT_SECRET.isEmpty()) {
                Log.e(TAG, "CLIENT_SECRET is empty! Please add your client secret from Spotify Developer Dashboard.")
                return AuthResult.Error("Configuration Error: CLIENT_SECRET is required. Please check SpotifyWebApiClient.kt")
            }
            
            val authHeader = createAuthHeader()
            val response = apiService.exchangeCodeForTokens(
                authorization = authHeader,
                code = authCode,
                redirectUri = AuthConstants.REDIRECT_URI
            )
            
            if (response.isSuccessful && response.body() != null) {
                val tokenResponse = response.body()!!
                val currentTime = System.currentTimeMillis()
                val expiresAt = currentTime + (tokenResponse.expiresIn * 1000L)
                
                val authTokens = AuthTokens(
                    accessToken = tokenResponse.accessToken,
                    refreshToken = tokenResponse.refreshToken,
                    expiresAt = expiresAt,
                    scopes = tokenResponse.scope.split(" ")
                )
                
                Log.d(TAG, "Token exchange successful. Expires at: ${java.util.Date(expiresAt)}")
                AuthResult.Success(authTokens)
            } else {
                val errorMessage = "Token exchange failed: ${response.code()} ${response.message()}"
                Log.e(TAG, errorMessage)
                AuthResult.Error(errorMessage)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error exchanging code for tokens", e)
            AuthResult.Error("Network error: ${e.message}")
        }
    }
    
    /**
     * Refresh access token using refresh token
     */
    suspend fun refreshAccessToken(refreshToken: String): AuthResult<AuthTokens> {
        return try {
            Log.d(TAG, "Refreshing access token")
            
            if (CLIENT_SECRET.isEmpty()) {
                Log.e(TAG, "CLIENT_SECRET is empty! Please add your client secret from Spotify Developer Dashboard.")
                return AuthResult.Error("Configuration Error: CLIENT_SECRET is required. Please check SpotifyWebApiClient.kt")
            }
            
            val authHeader = createAuthHeader()
            val response = apiService.refreshAccessToken(
                authorization = authHeader,
                refreshToken = refreshToken
            )
            
            if (response.isSuccessful && response.body() != null) {
                val tokenResponse = response.body()!!
                val currentTime = System.currentTimeMillis()
                val expiresAt = currentTime + (tokenResponse.expiresIn * 1000L)
                
                val authTokens = AuthTokens(
                    accessToken = tokenResponse.accessToken,
                    refreshToken = tokenResponse.refreshToken ?: refreshToken, // Keep original if not provided
                    expiresAt = expiresAt,
                    scopes = tokenResponse.scope.split(" ")
                )
                
                Log.d(TAG, "Token refresh successful. Expires at: ${java.util.Date(expiresAt)}")
                AuthResult.Success(authTokens)
            } else {
                val errorMessage = "Token refresh failed: ${response.code()} ${response.message()}"
                Log.e(TAG, errorMessage)
                AuthResult.Error(errorMessage)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error refreshing token", e)
            AuthResult.Error("Network error: ${e.message}")
        }
    }
    
    /**
     * Creates the Basic Authorization header for API requests
     */
    private fun createAuthHeader(): String {
        val credentials = "${AuthConstants.CLIENT_ID}:$CLIENT_SECRET"
        val encodedCredentials = Base64.encodeToString(credentials.toByteArray(), Base64.NO_WRAP)
        return "Basic $encodedCredentials"
    }
}