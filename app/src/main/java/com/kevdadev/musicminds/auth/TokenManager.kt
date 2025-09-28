package com.kevdadev.musicminds.auth

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.kevdadev.musicminds.auth.data.*

/**
 * Manages secure storage and retrieval of authentication tokens
 * Uses Android's EncryptedSharedPreferences for security
 */
class TokenManager(private val context: Context) {
    
    companion object {
        private const val TAG = "TokenManager"
    }
    
    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }
    
    private val encryptedPrefs: SharedPreferences by lazy {
        try {
            EncryptedSharedPreferences.create(
                context,
                AuthConstants.PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create encrypted preferences, falling back to regular prefs", e)
            // Fallback to regular SharedPreferences if encryption fails
            context.getSharedPreferences(AuthConstants.PREFS_NAME, Context.MODE_PRIVATE)
        }
    }
    
    /**
     * Saves authentication tokens securely
     */
    fun saveTokens(tokens: AuthTokens): Boolean {
        return try {
            Log.d(TAG, "Saving authentication tokens")
            encryptedPrefs.edit().apply {
                putString(AuthConstants.KEY_ACCESS_TOKEN, tokens.accessToken)
                tokens.refreshToken?.let { 
                    putString(AuthConstants.KEY_REFRESH_TOKEN, it)
                }
                putLong(AuthConstants.KEY_EXPIRES_AT, tokens.expiresAt)
                apply()
            }
            Log.d(TAG, "Tokens saved successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save tokens", e)
            false
        }
    }
    
    /**
     * Retrieves stored authentication tokens
     */
    fun getTokens(): AuthTokens? {
        return try {
            val accessToken = encryptedPrefs.getString(AuthConstants.KEY_ACCESS_TOKEN, null)
            val refreshToken = encryptedPrefs.getString(AuthConstants.KEY_REFRESH_TOKEN, null)
            val expiresAt = encryptedPrefs.getLong(AuthConstants.KEY_EXPIRES_AT, 0L)
            
            if (accessToken != null && expiresAt > 0) {
                Log.d(TAG, "Retrieved stored tokens (expires: ${java.util.Date(expiresAt)})")
                AuthTokens(
                    accessToken = accessToken,
                    refreshToken = refreshToken,
                    expiresAt = expiresAt,
                    scopes = AuthConstants.REQUIRED_SCOPES.toList()
                )
            } else {
                Log.d(TAG, "No valid tokens found in storage")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to retrieve tokens", e)
            null
        }
    }
    
    /**
     * Saves user information
     */
    fun saveUserInfo(userInfo: SpotifyUser): Boolean {
        return try {
            Log.d(TAG, "Saving user info: ${userInfo.displayName}")
            encryptedPrefs.edit().apply {
                putString(AuthConstants.KEY_USER_ID, userInfo.id)
                putString(AuthConstants.KEY_USER_DISPLAY_NAME, userInfo.displayName)
                putString(AuthConstants.KEY_USER_EMAIL, userInfo.email)
                apply()
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save user info", e)
            false
        }
    }
    
    /**
     * Retrieves stored user information
     */
    fun getUserInfo(): SpotifyUser? {
        return try {
            val userId = encryptedPrefs.getString(AuthConstants.KEY_USER_ID, null)
            val displayName = encryptedPrefs.getString(AuthConstants.KEY_USER_DISPLAY_NAME, null)
            val email = encryptedPrefs.getString(AuthConstants.KEY_USER_EMAIL, null)
            
            if (userId != null) {
                Log.d(TAG, "Retrieved user info: $displayName")
                SpotifyUser(
                    id = userId,
                    displayName = displayName,
                    email = email,
                    profileImageUrl = null, // TODO: Add profile image support
                    country = null,
                    product = null
                )
            } else {
                Log.d(TAG, "No user info found in storage")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to retrieve user info", e)
            null
        }
    }
    
    /**
     * Clears all stored authentication data
     */
    fun clearAllData(): Boolean {
        return try {
            Log.d(TAG, "Clearing all authentication data")
            encryptedPrefs.edit().clear().apply()
            Log.d(TAG, "Authentication data cleared successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear authentication data", e)
            false
        }
    }
    
    /**
     * Checks if there are any stored tokens
     */
    fun hasStoredTokens(): Boolean {
        val hasTokens = encryptedPrefs.contains(AuthConstants.KEY_ACCESS_TOKEN)
        Log.d(TAG, "Has stored tokens: $hasTokens")
        return hasTokens
    }
    
    /**
     * Updates only the access token (for token refresh)
     */
    fun updateAccessToken(accessToken: String, expiresAt: Long): Boolean {
        return try {
            Log.d(TAG, "Updating access token")
            encryptedPrefs.edit().apply {
                putString(AuthConstants.KEY_ACCESS_TOKEN, accessToken)
                putLong(AuthConstants.KEY_EXPIRES_AT, expiresAt)
                apply()
            }
            Log.d(TAG, "Access token updated successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update access token", e)
            false
        }
    }
}