package com.kevdadev.musicminds.domain.usecase

import android.util.Log
import com.kevdadev.musicminds.data.repository.SongRepository
import com.kevdadev.musicminds.domain.model.FlashcardSession
import com.kevdadev.musicminds.domain.model.FlashcardSessionState
import com.kevdadev.musicminds.domain.model.SessionConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Date

/**
 * Use case for creating a new flashcard session
 */
class CreateFlashcardSessionUseCase(
    private val songRepository: SongRepository,
    private val getSessionSongsUseCase: GetFlashcardSessionSongsUseCase
) {
    
    companion object {
        private const val TAG = "CreateFlashcardSession"
    }
    
    /**
     * Creates a new flashcard session for the user
     * 
     * @param userId The user ID to create session for
     * @param config Optional session configuration
     * @return Result containing the created session or error
     */
    suspend operator fun invoke(
        userId: String,
        config: SessionConfig = SessionConfig()
    ): Result<FlashcardSession> = withContext(Dispatchers.IO) {
        
        try {
            Log.d(TAG, "Creating flashcard session for user $userId")
            
            // Get songs for the session using the selection algorithm
            val songsResult = getSessionSongsUseCase(userId, config)
            
            if (songsResult.isFailure) {
                Log.e(TAG, "Failed to get songs for session", songsResult.exceptionOrNull())
                return@withContext Result.failure(
                    songsResult.exceptionOrNull() ?: Exception("Failed to select songs")
                )
            }
            
            val songSelection = songsResult.getOrNull()!!
            Log.d(TAG, "Selected ${songSelection.selectedSongs.size} songs for session")
            
            // Create the flashcard session
            val session = FlashcardSession(
                userId = userId,
                songQueue = songSelection.selectedSongs,
                sessionState = FlashcardSessionState.NOT_STARTED,
                sessionConfig = config,
                totalSongs = songSelection.selectedSongs.size
            )
            
            Log.d(TAG, "Created flashcard session with ${session.totalSongs} songs")
            Log.d(TAG, "Session breakdown - Learning: ${songSelection.learningCount}, To Learn: ${songSelection.toLearnCount}, Learned: ${songSelection.learnedCount}")
            
            Result.success(session)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error creating flashcard session", e)
            Result.failure(e)
        }
    }
}