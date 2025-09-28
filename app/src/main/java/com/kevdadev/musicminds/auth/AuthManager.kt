package com.kevdadev.musicminds.auth

import android.content.Context
import android.util.Log
import com.kevdadev.musicminds.auth.data.*
import com.spotify.sdk.android.auth.*

/**
 * Manages Spotify authentication flow using the Spotify Auth SDK
 */
class AuthManager(private val context: Context) {
    
    companion object {
        private const val TAG = "AuthManager"
    }
    
    /**
     * Creates an authentication request for Spotify authorization
     */
    fun createAuthRequest(): AuthorizationRequest {
        return AuthorizationRequest.Builder(
            AuthConstants.CLIENT_ID,
            AuthorizationResponse.Type.TOKEN,
            AuthConstants.REDIRECT_URI
        )
            .setScopes(AuthConstants.REQUIRED_SCOPES)
            .setShowDialog(true) // Force consent screen to show
            .build()
    }
    
    /**
     * Starts the Spotify authentication flow
     */
    fun startAuthFlow(): AuthorizationRequest {
        Log.d(TAG, "Starting Spotify authentication flow")
        val request = createAuthRequest()
        
        // Log the auth request details for debugging
        Log.d(TAG, "Client ID: ${AuthConstants.CLIENT_ID}")
        Log.d(TAG, "Redirect URI: ${AuthConstants.REDIRECT_URI}")
        Log.d(TAG, "Scopes: ${AuthConstants.REQUIRED_SCOPES.joinToString(", ")}")
        
        return request
    }
    
    /**
     * Processes the authentication response from Spotify
     */
    fun handleAuthResponse(response: AuthorizationResponse?): AuthResult<AuthTokens> {
        return when (response?.type) {
            AuthorizationResponse.Type.TOKEN -> {
                Log.d(TAG, "Authentication successful")
                val accessToken = response.accessToken
                val expiresIn = response.expiresIn // seconds
                val expiresAt = System.currentTimeMillis() + (expiresIn * 1000L)
                
                val tokens = AuthTokens(
                    accessToken = accessToken,
                    refreshToken = null, // TOKEN flow doesn't provide refresh tokens
                    expiresAt = expiresAt,
                    scopes = AuthConstants.REQUIRED_SCOPES.toList()
                )
                
                Log.d(TAG, "Token expires at: ${java.util.Date(expiresAt)}")
                AuthResult.Success(tokens)
            }
            
            AuthorizationResponse.Type.ERROR -> {
                val error = response.error ?: "Unknown authentication error"
                Log.e(TAG, "Authentication failed: $error")
                AuthResult.Error("Authentication failed: $error")
            }
            
            AuthorizationResponse.Type.EMPTY -> {
                Log.w(TAG, "Authentication cancelled by user")
                AuthResult.Error("Authentication cancelled")
            }
            
            else -> {
                Log.e(TAG, "Unknown authentication response type")
                AuthResult.Error("Unknown authentication response")
            }
        }
    }
    
    /**
     * Validates if the current tokens are still valid
     */
    fun isTokenValid(tokens: AuthTokens?): Boolean {
        if (tokens == null) {
            Log.d(TAG, "No tokens available")
            return false
        }
        
        val currentTime = System.currentTimeMillis()
        val isValid = currentTime < (tokens.expiresAt - AuthConstants.TOKEN_REFRESH_BUFFER_MS)
        
        Log.d(TAG, "Token valid: $isValid (expires: ${java.util.Date(tokens.expiresAt)})")
        return isValid
    }
    
    /**
     * Checks if token needs refresh (within buffer time)
     */
    fun needsRefresh(tokens: AuthTokens?): Boolean {
        if (tokens == null) return true
        
        val currentTime = System.currentTimeMillis()
        val needsRefresh = currentTime >= (tokens.expiresAt - AuthConstants.TOKEN_REFRESH_BUFFER_MS)
        
        Log.d(TAG, "Token needs refresh: $needsRefresh")
        return needsRefresh
    }
    
    /**
     * Attempts to refresh the access token
     * Note: TOKEN flow doesn't provide refresh tokens, so this will return an error
     * In a production app, you'd use CODE flow to get refresh tokens
     */
    fun refreshToken(tokens: AuthTokens): AuthResult<AuthTokens> {
        Log.w(TAG, "Token refresh not available with TOKEN flow - re-authentication required")
        return AuthResult.Error("Token refresh not available - please re-authenticate")
    }
    
    /**
     * Clears authentication state (logout)
     */
    fun logout(): AuthResult<Unit> {
        Log.d(TAG, "Logging out user")
        return AuthResult.Success(Unit)
    }
}