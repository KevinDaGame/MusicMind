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
     * Creates an authentication request for Spotify authorization using CODE flow
     */
    fun createAuthRequest(): AuthorizationRequest {
        return AuthorizationRequest.Builder(
            AuthConstants.CLIENT_ID,
            AuthorizationResponse.Type.CODE, // Changed from TOKEN to CODE
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
     * Processes the authentication response from Spotify (CODE flow)
     * Returns the authorization code which needs to be exchanged for tokens
     */
    fun handleAuthResponse(response: AuthorizationResponse?): AuthResult<String> {
        return when (response?.type) {
            AuthorizationResponse.Type.CODE -> {
                Log.d(TAG, "Authorization code received successfully")
                val authCode = response.code
                Log.d(TAG, "Authorization code: ${authCode.take(10)}...") // Log only first 10 chars for security
                AuthResult.Success(authCode)
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
     * Exchanges authorization code for access and refresh tokens using Web API
     */
    suspend fun exchangeCodeForTokens(authCode: String): AuthResult<AuthTokens> {
        Log.d(TAG, "Exchanging authorization code for tokens")
        return try {
            val webApiClient = com.kevdadev.musicminds.auth.api.SpotifyWebApiClient.getInstance()
            webApiClient.exchangeCodeForTokens(authCode)
        } catch (e: Exception) {
            Log.e(TAG, "Error during token exchange", e)
            AuthResult.Error("Token exchange failed: ${e.message}")
        }
    }

    /**
     * Refreshes the access token using the refresh token via Web API
     */
    suspend fun refreshToken(tokens: AuthTokens): AuthResult<AuthTokens> {
        Log.d(TAG, "Refreshing access token using refresh token")
        return try {
            if (tokens.refreshToken.isNullOrEmpty()) {
                Log.e(TAG, "No refresh token available")
                return AuthResult.Error("No refresh token available - please re-authenticate")
            }
            
            val webApiClient = com.kevdadev.musicminds.auth.api.SpotifyWebApiClient.getInstance()
            webApiClient.refreshAccessToken(tokens.refreshToken)
        } catch (e: Exception) {
            Log.e(TAG, "Error during token refresh", e)
            AuthResult.Error("Token refresh failed: ${e.message}")
        }
    }
    
    /**
     * Clears authentication state (logout)
     */
    fun logout(): AuthResult<Unit> {
        Log.d(TAG, "Logging out user")
        return AuthResult.Success(Unit)
    }
}