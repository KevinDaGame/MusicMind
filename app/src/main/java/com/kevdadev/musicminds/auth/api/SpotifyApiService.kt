package com.kevdadev.musicminds.auth.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * Retrofit interface for Spotify Web API token operations
 */
interface SpotifyApiService {
    
    /**
     * Exchange authorization code for access and refresh tokens
     */
    @FormUrlEncoded
    @POST("api/token")
    suspend fun exchangeCodeForTokens(
        @Header("Authorization") authorization: String, // Basic base64(client_id:client_secret)
        @Field("grant_type") grantType: String = "authorization_code",
        @Field("code") code: String,
        @Field("redirect_uri") redirectUri: String
    ): Response<SpotifyTokenResponse>
    
    /**
     * Refresh access token using refresh token
     */
    @FormUrlEncoded
    @POST("api/token")
    suspend fun refreshAccessToken(
        @Header("Authorization") authorization: String, // Basic base64(client_id:client_secret)
        @Field("grant_type") grantType: String = "refresh_token",
        @Field("refresh_token") refreshToken: String
    ): Response<SpotifyTokenResponse>
}