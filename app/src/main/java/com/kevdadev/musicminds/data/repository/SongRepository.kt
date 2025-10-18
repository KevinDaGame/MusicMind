package com.kevdadev.musicminds.data.repository

import android.util.Log
import com.kevdadev.musicminds.data.api.SpotifyApiService
import com.kevdadev.musicminds.data.database.dao.SongDao
import com.kevdadev.musicminds.data.database.entities.Song
import com.kevdadev.musicminds.data.database.entities.LearningStatus
import com.kevdadev.musicminds.data.database.entities.UserStatistics
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository that handles data operations for songs.
 * Provides a single point of access to both local database and Spotify API.
 */
class SongRepository(
    private val songDao: SongDao,
    private val spotifyApiService: SpotifyApiService
) {
    
    companion object {
        private const val TAG = "SongRepository"
    }
    
    /**
     * Search for songs on Spotify
     */
    suspend fun searchSpotifyTracks(query: String, limit: Int = 20): Result<List<Song>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Searching Spotify for: $query")
            
            val result = spotifyApiService.searchTracks(query, limit)
            
            result.fold(
                onSuccess = { searchResponse ->
                    val songs = searchResponse.tracks.items.map { spotifyTrack ->
                        spotifyApiService.mapSpotifyTrackToSong(spotifyTrack)
                    }
                    Log.d(TAG, "Found ${songs.size} tracks for query: $query")
                    Result.success(songs)
                },
                onFailure = { exception ->
                    Log.e(TAG, "Failed to search Spotify tracks", exception)
                    Result.failure(exception)
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Exception in searchSpotifyTracks", e)
            Result.failure(e)
        }
    }
    
    /**
     * Add a song to user's library
     * Returns true if successfully added, false if already exists
     */
    suspend fun addSongToUserLibrary(song: Song, userId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Adding song to user library: ${song.title} by ${song.artist}")
            
            val success = songDao.addSongToUserLibrary(song, userId)
            
            if (success) {
                Log.d(TAG, "Successfully added song to library: ${song.title}")
                Result.success(true)
            } else {
                Log.w(TAG, "Song already exists in user library: ${song.title}")
                Result.success(false)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add song to user library", e)
            Result.failure(e)
        }
    }
    
    /**
     * Check if a song already exists in the user's library
     */
    suspend fun isSongInUserLibrary(spotifyId: String, userId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val song = songDao.getSongBySpotifyId(spotifyId)
            if (song != null) {
                val userSong = songDao.getUserSong(userId, song.songId)
                return@withContext userSong != null
            }
            return@withContext false
        } catch (e: Exception) {
            Log.e(TAG, "Error checking if song is in user library", e)
            return@withContext false
        }
    }
    
    /**
     * Get user's songs by learning status
     */
    fun getUserSongsByStatus(userId: String, status: LearningStatus): Flow<List<Song>> {
        return songDao.getUserSongsByStatus(userId, status)
    }
    
    /**
     * Search within user's existing songs
     */
    suspend fun searchUserSongs(userId: String, query: String): List<Song> = withContext(Dispatchers.IO) {
        try {
            return@withContext songDao.searchUserSongs(userId, query)
        } catch (e: Exception) {
            Log.e(TAG, "Error searching user songs", e)
            return@withContext emptyList()
        }
    }
    
    /**
     * Get user statistics
     */
    suspend fun getUserStatistics(userId: String): UserStatistics? = withContext(Dispatchers.IO) {
        try {
            return@withContext songDao.getUserStatistics(userId)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user statistics", e)
            return@withContext null
        }
    }
}