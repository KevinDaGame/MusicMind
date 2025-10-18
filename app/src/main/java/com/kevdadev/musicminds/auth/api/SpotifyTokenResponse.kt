package com.kevdadev.musicminds.auth.api

import com.google.gson.annotations.SerializedName

/**
 * Response model for Spotify token exchange API
 */
data class SpotifyTokenResponse(
    @SerializedName("access_token")
    val accessToken: String,
    
    @SerializedName("token_type")
    val tokenType: String,
    
    @SerializedName("scope")
    val scope: String,
    
    @SerializedName("expires_in")
    val expiresIn: Int, // seconds
    
    @SerializedName("refresh_token")
    val refreshToken: String?
)

/**
 * Request model for token exchange
 */
data class TokenExchangeRequest(
    @SerializedName("grant_type")
    val grantType: String,
    
    @SerializedName("code")
    val code: String,
    
    @SerializedName("redirect_uri")
    val redirectUri: String,
    
    @SerializedName("client_id")
    val clientId: String,
    
    @SerializedName("client_secret")
    val clientSecret: String
)

/**
 * Request model for token refresh
 */
data class TokenRefreshRequest(
    @SerializedName("grant_type")
    val grantType: String,
    
    @SerializedName("refresh_token")
    val refreshToken: String,
    
    @SerializedName("client_id")
    val clientId: String,
    
    @SerializedName("client_secret")
    val clientSecret: String
)