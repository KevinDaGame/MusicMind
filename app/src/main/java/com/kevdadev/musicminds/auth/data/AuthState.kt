package com.kevdadev.musicminds.auth.data

/**
 * Represents the different states of user authentication in the app
 */
sealed class AuthState {
    /**
     * User is not authenticated and needs to go through auth flow
     */
    object Unauthenticated : AuthState()
    
    /**
     * Authentication is in progress
     */
    object Authenticating : AuthState()
    
    /**
     * User is successfully authenticated with valid tokens
     */
    data class Authenticated(val userInfo: SpotifyUser) : AuthState()
    
    /**
     * Authentication failed with an error
     */
    data class AuthError(val error: String, val canRetry: Boolean = true) : AuthState()
    
    /**
     * Authentication token has expired and needs refresh
     */
    object TokenExpired : AuthState()
    
    /**
     * Token refresh is in progress
     */
    object RefreshingToken : AuthState()
}

/**
 * Represents Spotify user information
 */
data class SpotifyUser(
    val id: String,
    val displayName: String?,
    val email: String?,
    val profileImageUrl: String?,
    val country: String?,
    val product: String? // free, premium, etc.
)

/**
 * Contains authentication tokens and metadata
 */
data class AuthTokens(
    val accessToken: String,
    val refreshToken: String?,
    val expiresAt: Long, // timestamp when token expires
    val scopes: List<String>
)

/**
 * Result wrapper for authentication operations
 */
sealed class AuthResult<out T> {
    data class Success<T>(val data: T) : AuthResult<T>()
    data class Error(val message: String, val cause: Throwable? = null) : AuthResult<Nothing>()
    object Loading : AuthResult<Nothing>()
}