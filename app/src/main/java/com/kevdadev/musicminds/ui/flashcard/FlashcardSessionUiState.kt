package com.kevdadev.musicminds.ui.flashcard

import com.kevdadev.musicminds.domain.model.FlashcardSession

/**
 * UI states for the flashcard session
 */
sealed class FlashcardSessionUiState {
    
    /**
     * Loading state with optional message
     */
    data class Loading(val message: String = "Loading...") : FlashcardSessionUiState()
    
    /**
     * Session is ready to start
     */
    data class Ready(val session: FlashcardSession) : FlashcardSessionUiState()
    
    /**
     * Session is in progress
     */
    data class InProgress(val session: FlashcardSession) : FlashcardSessionUiState()
    
    /**
     * Answer has been revealed for current song
     */
    data class AnswerRevealed(val session: FlashcardSession) : FlashcardSessionUiState()
    
    /**
     * Session completed
     */
    data class Completed(val session: FlashcardSession) : FlashcardSessionUiState()
    
    /**
     * Error state
     */
    data class Error(val message: String, val canRetry: Boolean = true) : FlashcardSessionUiState()
}