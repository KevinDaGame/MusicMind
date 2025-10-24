package com.kevdadev.musicminds.domain.model

import java.util.Date

/**
 * Represents a user's answer to a flashcard during a session
 */
data class FlashcardAnswer(
    val sessionId: String,
    val songId: String,
    val titleGuess: String? = null,
    val artistGuess: String? = null,
    val yearGuess: Int? = null,
    val isCorrect: Boolean? = null, // For self-reporting system
    val answeredAt: Date = Date(),
    val timeToAnswerMs: Long? = null
)

/**
 * Session progress tracking
 */
data class SessionProgress(
    val currentSongNumber: Int,
    val totalSongs: Int,
    val correctCount: Int,
    val incorrectCount: Int,
    val skippedCount: Int
) {
    val completionPercentage: Float
        get() = if (totalSongs > 0) {
            (currentSongNumber.toFloat() / totalSongs) * 100f
        } else 0f
    
    val accuracy: Float
        get() = if (correctCount + incorrectCount > 0) {
            correctCount.toFloat() / (correctCount + incorrectCount)
        } else 0f
}