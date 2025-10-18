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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

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
    
    // Coroutine scope for async operations
    private val authScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
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
                Log.d(TAG, "Stored tokens are expired, attempting refresh")
                if (tokens.refreshToken != null) {
                    _authState.value = AuthState.RefreshingToken
                    authScope.launch {
                        refreshTokens(tokens)
                    }
                } else {
                    Log.d(TAG, "No refresh token available, clearing tokens")
                    tokenManager.clearAllData()
                    _authState.value = AuthState.Unauthenticated
                }
            }
            authManager.needsRefresh(tokens) -> {
                Log.d(TAG, "Tokens need refresh")
                _authState.value = AuthState.RefreshingToken
                authScope.launch {
                    refreshTokens(tokens)
                }
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
     * Handles the authentication response from Spotify (CODE flow)
     */
    suspend fun handleAuthResponse(response: AuthorizationResponse?): Boolean {
        Log.d(TAG, "Handling authentication response")
        
        val codeResult = authManager.handleAuthResponse(response)
        
        return when (codeResult) {
            is AuthResult.Success -> {
                Log.d(TAG, "Authorization code received, exchanging for tokens")
                _authState.value = AuthState.Authenticating
                
                // Exchange authorization code for access and refresh tokens
                val tokenResult = authManager.exchangeCodeForTokens(codeResult.data)
                
                when (tokenResult) {
                    is AuthResult.Success -> {
                        Log.d(TAG, "Token exchange successful, saving tokens")
                        val saved = tokenManager.saveTokens(tokenResult.data)
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
                        Log.e(TAG, "Token exchange failed: ${tokenResult.message}")
                        _authState.value = AuthState.AuthError("Token exchange failed: ${tokenResult.message}")
                        false
                    }
                    else -> {
                        _authState.value = AuthState.AuthError("Unexpected token exchange response")
                        false
                    }
                }
            }
            is AuthResult.Error -> {
                Log.e(TAG, "Authorization failed: ${codeResult.message}")
                _authState.value = AuthState.AuthError(codeResult.message)
                false
            }
            is AuthResult.Loading -> {
                _authState.value = AuthState.Authenticating
                false
            }
        }
    }
    
    /**
     * Attempts to refresh authentication tokens using refresh token
     */
    private suspend fun refreshTokens(tokens: AuthTokens) {
        Log.d(TAG, "Attempting to refresh tokens")
        
        try {
            val result = authManager.refreshToken(tokens)
            
            when (result) {
                is AuthResult.Success -> {
                    Log.d(TAG, "Token refresh successful")
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
                    // Clear tokens and require reauthentication
                    tokenManager.clearAllData()
                    _authState.value = AuthState.Unauthenticated
                }
                else -> {
                    _authState.value = AuthState.AuthError("Unexpected token refresh response")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during token refresh", e)
            // Clear tokens and require reauthentication on any exception
            tokenManager.clearAllData()
            _authState.value = AuthState.Unauthenticated
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
     * Automatically triggers refresh if token is expired and refresh token is available
     */
    fun getValidAccessToken(): String? {
        val tokens = tokenManager.getTokens()
        return if (tokens != null && authManager.isTokenValid(tokens)) {
            tokens.accessToken
        } else if (tokens != null && !tokens.refreshToken.isNullOrEmpty()) {
            // Token is expired but we have a refresh token - attempt automatic refresh
            Log.d(TAG, "Access token expired, attempting automatic refresh")
            _authState.value = AuthState.RefreshingToken
            authScope.launch {
                refreshTokens(tokens)
            }
            null // Return null for now, the caller should observe auth state for updates
        } else {
            // No tokens or no refresh token available - require reauthentication
            if (tokens != null) {
                Log.w(TAG, "Access token expired and no refresh token available")
                tokenManager.clearAllData()
                _authState.value = AuthState.Unauthenticated
            }
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