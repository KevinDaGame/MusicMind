package com.kevdadev.musicminds.domain.model

import android.os.Parcelable
import com.kevdadev.musicminds.data.database.entities.Song
import kotlinx.parcelize.Parcelize
import java.util.Date
import java.util.UUID

/**
 * Represents a flashcard learning session
 */
@Parcelize
data class FlashcardSession(
    val sessionId: String = UUID.randomUUID().toString(),
    val userId: String,
    val songQueue: List<Song>,
    val currentSongIndex: Int = 0,
    val sessionState: FlashcardSessionState = FlashcardSessionState.NOT_STARTED,
    val startedAt: Date? = null,
    val completedAt: Date? = null,
    val totalSongs: Int = songQueue.size,
    val correctAnswers: Int = 0,
    val sessionConfig: SessionConfig = SessionConfig()
) : Parcelable {
    val currentSong: Song?
        get() = if (currentSongIndex < songQueue.size) songQueue[currentSongIndex] else null
    
    val progress: Float
        get() = if (totalSongs > 0) currentSongIndex.toFloat() / totalSongs else 0f
    
    val isCompleted: Boolean
        get() = currentSongIndex >= totalSongs || sessionState == FlashcardSessionState.COMPLETED
    
    val songsRemaining: Int
        get() = maxOf(0, totalSongs - currentSongIndex)
}

/**
 * Session state enumeration
 */
enum class FlashcardSessionState {
    NOT_STARTED,
    IN_PROGRESS,
    PAUSED,
    COMPLETED,
    CANCELLED
}

/**
 * Configuration for flashcard session algorithm
 */
@Parcelize
data class SessionConfig(
    val targetSongCount: Int = 10,
    val learningPercentage: Float = 0.7f,  // 70%
    val toLearnPercentage: Float = 0.2f,   // 20%
    val learnedPercentage: Float = 0.1f,   // 10%
    val minSongsRequired: Int = 3
) : Parcelable {
    init {
        require(learningPercentage + toLearnPercentage + learnedPercentage == 1.0f) {
            "Percentages must sum to 1.0"
        }
        require(targetSongCount >= minSongsRequired) {
            "Target song count must be at least $minSongsRequired"
        }
    }
}

/**
 * Result of song selection algorithm
 */
data class SongSelectionResult(
    val selectedSongs: List<Song>,
    val learningCount: Int,
    val toLearnCount: Int,
    val learnedCount: Int,
    val totalAvailable: SongCategoryAvailability
)

/**
 * Available songs by category for session planning
 */
data class SongCategoryAvailability(
    val learningAvailable: Int,
    val toLearnAvailable: Int,
    val learnedAvailable: Int
) {
    val totalAvailable: Int
        get() = learningAvailable + toLearnAvailable + learnedAvailable
}