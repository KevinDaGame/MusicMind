package com.kevdadev.musicminds.auth.data

/**
 * Constants for authentication configuration
 */
object AuthConstants {
    const val CLIENT_ID = "0897452c456b4f468a7d1ca8bc42f535"
    const val REDIRECT_URI = "musicminds://callback"
    
    // Spotify scopes needed for the app
    val REQUIRED_SCOPES = arrayOf(
        "user-read-private",        // Access user profile information
        "user-read-email",          // Access user email
        "streaming",                // Control playback in Spotify Connect devices
        "app-remote-control"        // Remote control Spotify app
    )
    
    // SharedPreferences keys
    const val PREFS_NAME = "musicminds_auth_prefs"
    const val KEY_ACCESS_TOKEN = "access_token"
    const val KEY_REFRESH_TOKEN = "refresh_token"
    const val KEY_EXPIRES_AT = "expires_at"
    const val KEY_USER_ID = "user_id"
    const val KEY_USER_DISPLAY_NAME = "user_display_name"
    const val KEY_USER_EMAIL = "user_email"
    
    // KeyStore alias for encryption
    const val KEYSTORE_ALIAS = "musicminds_auth_key"
    
    // Request codes
    const val AUTH_REQUEST_CODE = 1001
    
    // Token refresh buffer (refresh 5 minutes before expiry)
    const val TOKEN_REFRESH_BUFFER_MS = 5 * 60 * 1000L
}