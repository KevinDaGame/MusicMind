package com.kevdadev.musicminds.data.api

import android.util.Log
import com.google.gson.Gson
import com.kevdadev.musicminds.auth.TokenManager
import com.kevdadev.musicminds.data.api.models.SpotifySearchResponse
import com.kevdadev.musicminds.data.database.entities.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Calendar

class SpotifyApiService(private val tokenManager: TokenManager) {
    
    companion object {
        private const val TAG = "SpotifyApiService"
        private const val SPOTIFY_API_BASE_URL = "https://api.spotify.com/v1"
        private const val DEFAULT_LIMIT = 20
    }
    
    private val gson = Gson()
    
    /**
     * Search for tracks on Spotify
     */
    suspend fun searchTracks(
        query: String,
        limit: Int = DEFAULT_LIMIT,
        offset: Int = 0
    ): Result<SpotifySearchResponse> = withContext(Dispatchers.IO) {
        try {
            val tokens = tokenManager.getTokens()
            val accessToken = tokens?.accessToken
            if (accessToken.isNullOrBlank()) {
                return@withContext Result.failure(SecurityException("No valid access token available"))
            }
            
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = URL("$SPOTIFY_API_BASE_URL/search?q=$encodedQuery&type=track&limit=$limit&offset=$offset")
            
            val connection = url.openConnection() as HttpURLConnection
            connection.apply {
                requestMethod = "GET"
                setRequestProperty("Authorization", "Bearer $accessToken")
                setRequestProperty("Content-Type", "application/json")
                connectTimeout = 10000
                readTimeout = 10000
            }
            
            val responseCode = connection.responseCode
            Log.d(TAG, "Search API response code: $responseCode")
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                Log.d(TAG, "Search API response: ${response.take(200)}...")
                
                val searchResponse = gson.fromJson(response, SpotifySearchResponse::class.java)
                Result.success(searchResponse)
            } else {
                val errorResponse = connection.errorStream?.bufferedReader()?.use { it.readText() }
                Log.e(TAG, "Search API error: $responseCode - $errorResponse")
                Result.failure(IOException("Search failed with response code: $responseCode"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Search API exception", e)
            Result.failure(e)
        }
    }
    
    /**
     * Convert Spotify API track to local Song entity
     */
    fun mapSpotifyTrackToSong(spotifyTrack: com.kevdadev.musicminds.data.api.models.SpotifyTrack): Song {
        // Extract year from release date (format: YYYY-MM-DD or YYYY)
        val releaseYear = try {
            val releaseDate = spotifyTrack.album.releaseDate
            if (releaseDate.length >= 4) {
                releaseDate.substring(0, 4).toInt()
            } else {
                Calendar.getInstance().get(Calendar.YEAR) // Default to current year if parsing fails
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse release year: ${spotifyTrack.album.releaseDate}", e)
            Calendar.getInstance().get(Calendar.YEAR)
        }
        
        // Get the best image URL (prefer medium size)
        val imageUrl = spotifyTrack.album.images.let { images ->
            when {
                images.isEmpty() -> null
                images.size == 1 -> images[0].url
                else -> {
                    // Try to find medium size image (around 300px)
                    images.find { it.height != null && it.height in 250..350 }?.url
                        ?: images.find { it.height != null && it.height in 200..400 }?.url
                        ?: images[0].url // Fallback to first image
                }
            }
        }
        
        // Combine artist names if multiple artists
        val artistNames = spotifyTrack.artists.joinToString(", ") { it.name }
        
        return Song(
            spotifyId = spotifyTrack.id,
            title = spotifyTrack.name,
            artist = artistNames,
            album = spotifyTrack.album.name,
            releaseYear = releaseYear,
            durationMs = spotifyTrack.durationMs,
            previewUrl = spotifyTrack.previewUrl,
            imageUrl = imageUrl
        )
    }
}