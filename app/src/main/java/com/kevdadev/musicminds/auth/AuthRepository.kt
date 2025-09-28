package com.kevdadev.musicminds.auth

import android.content.Context
import android.content.Intent
import android.util.Log
import com.kevdadev.musicminds.auth.data.*
import com.spotify.sdk.android.auth.AuthorizationClient
import com.spotify.sdk.android.auth.AuthorizationRequest
import com.spotify.sdk.android.auth.AuthorizationResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Repository that manages authentication state and operations
 * This is the single source of truth for authentication in the app
 */
class AuthRepository(
    private val authManager: AuthManager,
    private val tokenManager: TokenManager
) {
    
    companion object {
        private const val TAG = "AuthRepository"
        
        @Volatile
        private var INSTANCE: AuthRepository? = null
        
        fun getInstance(context: Context): AuthRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = AuthRepository(
                    AuthManager(context.applicationContext),
                    TokenManager(context.applicationContext)
                )
                INSTANCE = instance
                instance
            }
        }
    }
    
    private val _authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()
    
    init {
        // Check if user is already authenticated on initialization
        checkAuthenticationStatus()
    }
    
    /**
     * Checks current authentication status on app start
     */
    fun checkAuthenticationStatus() {
        Log.d(TAG, "Checking authentication status")
        
        val tokens = tokenManager.getTokens()
        val userInfo = tokenManager.getUserInfo()
        
        when {
            tokens == null || userInfo == null -> {
                Log.d(TAG, "No stored authentication found")
                _authState.value = AuthState.Unauthenticated
            }
            !authManager.isTokenValid(tokens) -> {
                Log.d(TAG, "Stored tokens are expired")
                _authState.value = AuthState.TokenExpired
            }
            authManager.needsRefresh(tokens) -> {
                Log.d(TAG, "Tokens need refresh")
                _authState.value = AuthState.RefreshingToken
                refreshTokens(tokens)
            }
            else -> {
                Log.d(TAG, "User is authenticated with valid tokens")
                _authState.value = AuthState.Authenticated(userInfo)
            }
        }
    }
    
    /**
     * Starts the authentication flow
     */
    fun startAuthentication() {
        Log.d(TAG, "Starting authentication flow")
        _authState.value = AuthState.Authenticating
    }
    
    /**
     * Handles the authentication response from Spotify
     */
    fun handleAuthResponse(response: AuthorizationResponse?): Boolean {
        Log.d(TAG, "Handling authentication response")
        
        val result = authManager.handleAuthResponse(response)
        
        return when (result) {
            is AuthResult.Success -> {
                Log.d(TAG, "Authentication successful, saving tokens")
                val saved = tokenManager.saveTokens(result.data)
                if (saved) {
                    // TODO: Fetch user info from Spotify Web API
                    // For now, create a basic user object
                    val userInfo = SpotifyUser(
                        id = "temp_user_id",
                        displayName = "Spotify User",
                        email = null,
                        profileImageUrl = null,
                        country = null,
                        product = null
                    )
                    tokenManager.saveUserInfo(userInfo)
                    _authState.value = AuthState.Authenticated(userInfo)
                    true
                } else {
                    _authState.value = AuthState.AuthError("Failed to save authentication tokens")
                    false
                }
            }
            is AuthResult.Error -> {
                Log.e(TAG, "Authentication failed: ${result.message}")
                _authState.value = AuthState.AuthError(result.message)
                false
            }
            is AuthResult.Loading -> {
                _authState.value = AuthState.Authenticating
                false
            }
        }
    }
    
    /**
     * Attempts to refresh authentication tokens
     */
    private fun refreshTokens(tokens: AuthTokens) {
        Log.d(TAG, "Attempting to refresh tokens")
        
        val result = authManager.refreshToken(tokens)
        
        when (result) {
            is AuthResult.Success -> {
                val saved = tokenManager.saveTokens(result.data)
                if (saved) {
                    val userInfo = tokenManager.getUserInfo()
                    if (userInfo != null) {
                        _authState.value = AuthState.Authenticated(userInfo)
                    } else {
                        _authState.value = AuthState.AuthError("User info not found after refresh")
                    }
                } else {
                    _authState.value = AuthState.AuthError("Failed to save refreshed tokens")
                }
            }
            is AuthResult.Error -> {
                Log.w(TAG, "Token refresh failed: ${result.message}")
                // For TOKEN flow, refresh isn't available, so user needs to re-authenticate
                _authState.value = AuthState.TokenExpired
            }
            else -> {
                _authState.value = AuthState.AuthError("Unexpected token refresh response")
            }
        }
    }
    
    /**
     * Logs out the current user
     */
    fun logout() {
        Log.d(TAG, "Logging out user")
        
        authManager.logout()
        tokenManager.clearAllData()
        _authState.value = AuthState.Unauthenticated
    }
    
    /**
     * Creates an authentication request for Spotify OAuth
     */
    fun createAuthRequest(): AuthorizationRequest {
        return authManager.createAuthRequest()
    }
    
    /**
     * Handles the authentication response from the authorization activity
     */
    suspend fun handleAuthResponse(requestCode: Int, resultCode: Int, data: Intent?) {
        Log.d(TAG, "Handling auth response - requestCode: $requestCode, resultCode: $resultCode")
        
        _authState.value = AuthState.Authenticating
        
        try {
            val response = AuthorizationClient.getResponse(resultCode, data)
            val success = handleAuthResponse(response)
            
            if (!success) {
                Log.e(TAG, "Failed to handle auth response")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling auth response", e)
            _authState.value = AuthState.AuthError("Authentication failed: ${e.message}")
        }
    }
    
    /**
     * Gets the current access token if available and valid
     */
    fun getValidAccessToken(): String? {
        val tokens = tokenManager.getTokens()
        return if (tokens != null && authManager.isTokenValid(tokens)) {
            tokens.accessToken
        } else {
            null
        }
    }
    
    /**
     * Gets the current user info if authenticated
     */
    fun getCurrentUser(): SpotifyUser? {
        return when (val state = _authState.value) {
            is AuthState.Authenticated -> state.userInfo
            else -> null
        }
    }
    
    /**
     * Checks if user is currently authenticated
     */
    fun isAuthenticated(): Boolean {
        return _authState.value is AuthState.Authenticated
    }
    
    /**
     * Retries authentication after an error
     */
    fun retryAuthentication() {
        Log.d(TAG, "Retrying authentication")
        _authState.value = AuthState.Unauthenticated
        startAuthentication()
    }
}