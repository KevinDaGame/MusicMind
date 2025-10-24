package com.kevdadev.musicminds.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.kevdadev.musicminds.data.database.entities.Song
import com.kevdadev.musicminds.data.database.entities.UserSong
import com.kevdadev.musicminds.data.database.entities.UserStatistics
import com.kevdadev.musicminds.data.database.entities.LearningStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {
    
    @Query("SELECT COUNT(*) FROM songs WHERE spotify_id = :spotifyId")
    suspend fun songExists(spotifyId: String): Int
    
    @Query("SELECT * FROM songs WHERE spotify_id = :spotifyId LIMIT 1")
    suspend fun getSongBySpotifyId(spotifyId: String): Song?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSong(song: Song): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserSong(userSong: UserSong): Long
    
    @Transaction
    suspend fun addSongToUserLibrary(song: Song, userId: String): Boolean {
        return try {
            // Check if song already exists in user's library
            val existingUserSong = getUserSong(userId, song.songId)
            if (existingUserSong != null) {
                return false // Song already exists in user's library
            }
            
            // Insert song if it doesn't exist
            var songToUse = getSongBySpotifyId(song.spotifyId)
            if (songToUse == null) {
                insertSong(song)
                songToUse = song
            }
            
            // Add to user's library
            val userSong = UserSong(
                userId = userId,
                songId = songToUse.songId,
                learningStatus = LearningStatus.TO_LEARN
            )
            insertUserSong(userSong)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    @Query("""
        SELECT s.* FROM songs s 
        JOIN user_songs us ON s.song_id = us.song_id 
        WHERE us.user_id = :userId AND us.learning_status = :status
        ORDER BY us.status_changed_at DESC
    """)
    fun getUserSongsByStatus(userId: String, status: LearningStatus): Flow<List<Song>>
    
    @Query("""
        SELECT s.* FROM songs s 
        JOIN user_songs us ON s.song_id = us.song_id 
        WHERE us.user_id = :userId AND (s.title LIKE '%' || :query || '%' OR s.artist LIKE '%' || :query || '%')
    """)
    suspend fun searchUserSongs(userId: String, query: String): List<Song>
    
    @Query("SELECT * FROM user_songs WHERE user_id = :userId AND song_id = :songId LIMIT 1")
    suspend fun getUserSong(userId: String, songId: String): UserSong?
    
    @Query("""
        SELECT COUNT(*) as totalSongs,
        SUM(CASE WHEN learning_status = 'LEARNED' THEN 1 ELSE 0 END) as learnedSongs,
        SUM(CASE WHEN learning_status = 'LEARNING' THEN 1 ELSE 0 END) as learningSongs,
        SUM(CASE WHEN learning_status = 'TO_LEARN' THEN 1 ELSE 0 END) as toLearnSongs,
        SUM(points_earned) as totalPoints
        FROM user_songs WHERE user_id = :userId
    """)
    suspend fun getUserStatistics(userId: String): UserStatistics?
    
    @Query("UPDATE user_songs SET learning_status = :newStatus, status_changed_at = :statusChangedAt WHERE user_id = :userId AND song_id = :songId")
    suspend fun updateUserSongLearningStatus(userId: String, songId: String, newStatus: LearningStatus, statusChangedAt: java.util.Date)
    
    @Query("""
        SELECT s.* FROM songs s 
        JOIN user_songs us ON s.song_id = us.song_id 
        WHERE us.user_id = :userId AND us.learning_status = :status
        ORDER BY RANDOM()
        LIMIT :limit
    """)
    suspend fun getRandomSongsByStatus(userId: String, status: LearningStatus, limit: Int): List<Song>
    
    @Query("""
        SELECT 
        SUM(CASE WHEN learning_status = 'TO_LEARN' THEN 1 ELSE 0 END) as toLearnCount,
        SUM(CASE WHEN learning_status = 'LEARNING' THEN 1 ELSE 0 END) as learningCount,
        SUM(CASE WHEN learning_status = 'LEARNED' THEN 1 ELSE 0 END) as learnedCount
        FROM user_songs WHERE user_id = :userId
    """)
    suspend fun getCategoryCounts(userId: String): CategoryCounts?
    
    data class CategoryCounts(
        val toLearnCount: Int,
        val learningCount: Int,
        val learnedCount: Int
    )
}