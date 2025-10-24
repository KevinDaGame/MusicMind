package com.kevdadev.musicminds.domain.usecase

import android.util.Log
import com.kevdadev.musicminds.data.database.entities.LearningStatus
import com.kevdadev.musicminds.data.database.entities.Song
import com.kevdadev.musicminds.data.repository.SongRepository
import com.kevdadev.musicminds.domain.model.SessionConfig
import com.kevdadev.musicminds.domain.model.SongCategoryAvailability
import com.kevdadev.musicminds.domain.model.SongSelectionResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Use case for selecting songs for a flashcard session based on the learning algorithm
 */
class GetFlashcardSessionSongsUseCase(
    private val songRepository: SongRepository
) {
    
    companion object {
        private const val TAG = "GetFlashcardSessionSongs"
    }

    /**
     * Selects songs for a flashcard session based on the configured algorithm
     * - 70% from "Learning" category
     * - 20% from "To Learn" category  
     * - 10% from "Learned" category
     * 
     * If there are insufficient songs in "Learning", backfills from "To Learn"
     * 
     * @param userId The user ID to get songs for
     * @param config Session configuration with percentages and target count
     * @return Result containing selected songs and category breakdown
     */
    suspend operator fun invoke(
        userId: String, 
        config: SessionConfig = SessionConfig()
    ): Result<SongSelectionResult> = withContext(Dispatchers.IO) {
        
        try {
            Log.d(TAG, "Starting song selection for user $userId with config: $config")
            
            // Get available songs by category
            val availability = getAvailableSongs(userId)
            Log.d(TAG, "Available songs: $availability")
            
            // Check if we have enough songs total
            if (availability.totalAvailable < config.minSongsRequired) {
                Log.w(TAG, "Insufficient songs available: ${availability.totalAvailable} < ${config.minSongsRequired}")
                return@withContext Result.failure(
                    InsufficientSongsException("Need at least ${config.minSongsRequired} songs in library")
                )
            }
            
            // Calculate target counts for each category
            val targetCounts = calculateTargetCounts(config, availability)
            Log.d(TAG, "Target counts: $targetCounts")
            
            // Select songs from each category
            val selectedSongs = mutableListOf<Song>()
            var actualCounts = CategoryCounts()
            
            // 1. Select Learning songs
            if (targetCounts.learning > 0) {
                val learningSongs = songRepository.getRandomSongsByStatus(
                    userId, LearningStatus.LEARNING, targetCounts.learning
                )
                selectedSongs.addAll(learningSongs)
                actualCounts = actualCounts.copy(learning = learningSongs.size)
                Log.d(TAG, "Selected ${learningSongs.size} Learning songs")
            }
            
            // 2. Select To Learn songs (including backfill if Learning was insufficient)
            val toLearnTarget = targetCounts.toLearn + targetCounts.learningBackfill
            if (toLearnTarget > 0) {
                val toLearnSongs = songRepository.getRandomSongsByStatus(
                    userId, LearningStatus.TO_LEARN, toLearnTarget
                )
                selectedSongs.addAll(toLearnSongs)
                actualCounts = actualCounts.copy(toLearn = toLearnSongs.size)
                Log.d(TAG, "Selected ${toLearnSongs.size} To Learn songs (including ${targetCounts.learningBackfill} backfill)")
            }
            
            // 3. Select Learned songs  
            if (targetCounts.learned > 0) {
                val learnedSongs = songRepository.getRandomSongsByStatus(
                    userId, LearningStatus.LEARNED, targetCounts.learned
                )
                selectedSongs.addAll(learnedSongs)
                actualCounts = actualCounts.copy(learned = learnedSongs.size)
                Log.d(TAG, "Selected ${learnedSongs.size} Learned songs")
            }
            
            // Shuffle the final list to mix categories
            val shuffledSongs = selectedSongs.shuffled()
            
            val result = SongSelectionResult(
                selectedSongs = shuffledSongs,
                learningCount = actualCounts.learning,
                toLearnCount = actualCounts.toLearn,
                learnedCount = actualCounts.learned,
                totalAvailable = availability
            )
            
            Log.d(TAG, "Final selection: ${shuffledSongs.size} songs total")
            Log.d(TAG, "Category breakdown - Learning: ${actualCounts.learning}, To Learn: ${actualCounts.toLearn}, Learned: ${actualCounts.learned}")
            
            Result.success(result)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error selecting songs for session", e)
            Result.failure(e)
        }
    }
    
    private suspend fun getAvailableSongs(userId: String): SongCategoryAvailability {
        val categoryCounts = songRepository.getCategoryCounts(userId)
        return SongCategoryAvailability(
            learningAvailable = categoryCounts.learningCount,
            toLearnAvailable = categoryCounts.toLearnCount,
            learnedAvailable = categoryCounts.learnedCount
        )
    }
    
    private fun calculateTargetCounts(
        config: SessionConfig, 
        availability: SongCategoryAvailability
    ): TargetCounts {
        
        // Calculate ideal counts based on percentages
        val targetLearning = (config.targetSongCount * config.learningPercentage).toInt()
        val targetToLearn = (config.targetSongCount * config.toLearnPercentage).toInt()
        val targetLearned = (config.targetSongCount * config.learnedPercentage).toInt()
        
        // Adjust for available songs and handle backfill
        val actualLearning = minOf(targetLearning, availability.learningAvailable)
        val learningShortfall = targetLearning - actualLearning
        val adjustedToLearn = minOf(
            targetToLearn + learningShortfall, 
            availability.toLearnAvailable
        )
        val actualLearned = minOf(targetLearned, availability.learnedAvailable)
        
        return TargetCounts(
            learning = actualLearning,
            toLearn = adjustedToLearn,
            learned = actualLearned,
            learningBackfill = learningShortfall
        )
    }
    
    private data class TargetCounts(
        val learning: Int,
        val toLearn: Int,
        val learned: Int,
        val learningBackfill: Int = 0
    )
    
    private data class CategoryCounts(
        val learning: Int = 0,
        val toLearn: Int = 0,
        val learned: Int = 0
    )
}

/**
 * Exception thrown when user doesn't have enough songs for a session
 */
class InsufficientSongsException(message: String) : Exception(message)