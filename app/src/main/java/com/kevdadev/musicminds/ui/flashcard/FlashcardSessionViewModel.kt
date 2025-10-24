package com.kevdadev.musicminds.ui.flashcard

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kevdadev.musicminds.auth.AuthRepository
import com.kevdadev.musicminds.auth.TokenManager
import com.kevdadev.musicminds.data.api.SpotifyApiService
import com.kevdadev.musicminds.data.database.AppDatabase
import com.kevdadev.musicminds.data.repository.SongRepository
import com.kevdadev.musicminds.domain.model.FlashcardSession
import com.kevdadev.musicminds.domain.model.FlashcardSessionState
import com.kevdadev.musicminds.domain.model.SessionConfig
import com.kevdadev.musicminds.domain.usecase.CreateFlashcardSessionUseCase
import com.kevdadev.musicminds.domain.usecase.GetFlashcardSessionSongsUseCase
import com.kevdadev.musicminds.domain.usecase.InsufficientSongsException
import com.kevdadev.musicminds.audio.SpotifyPlaybackManager
import com.kevdadev.musicminds.domain.model.PlaybackCommand
import com.kevdadev.musicminds.domain.model.PlaybackState
import com.spotify.protocol.types.PlayerState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date

/**
 * ViewModel for managing flashcard session state and user interactions with Spotify playback
 */
class FlashcardSessionViewModel(application: Application) : AndroidViewModel(application) {
    
    companion object {
        private const val TAG = "FlashcardSessionVM"
    }
    
    // Dependencies
    private val authRepository = AuthRepository.getInstance(application)
    private val database = AppDatabase.getDatabase(application)
    private val songRepository = SongRepository(
        database.songDao(), 
        SpotifyApiService(TokenManager(application))
    )
    private val spotifyPlaybackManager = SpotifyPlaybackManager(application)
    
    // Use cases
    private val getSessionSongsUseCase = GetFlashcardSessionSongsUseCase(songRepository)
    private val createSessionUseCase = CreateFlashcardSessionUseCase(songRepository, getSessionSongsUseCase)
    
    // State
    private val _sessionState = MutableStateFlow<FlashcardSessionUiState>(
        FlashcardSessionUiState.Loading("Preparing session...")
    )
    val sessionState: StateFlow<FlashcardSessionUiState> = _sessionState.asStateFlow()
    
    // Playback state
    private val _playbackState = MutableStateFlow<PlaybackUiState>(PlaybackUiState.Disconnected)
    val playbackState: StateFlow<PlaybackUiState> = _playbackState.asStateFlow()
    
    private var currentSession: FlashcardSession? = null
    private var exitRequested = false
    private var hasAutoStartedCurrentSong = false
    
    init {
        // Observe Spotify playback state (includes connection state)
        viewModelScope.launch {
            spotifyPlaybackManager.playbackState.collect { playbackState ->
                updatePlaybackUiState(playbackState)
                
                // Only handle auto-start on initial connection, not on every state change
                if (playbackState.isConnected && !hasAutoStartedCurrentSong) {
                    onSpotifyConnected()
                }
            }
        }
    }
    
    /**
     * UI state for Spotify playback
     */
    sealed class PlaybackUiState {
        object Disconnected : PlaybackUiState()
        object Connected : PlaybackUiState()
        data class Playing(
            val trackName: String,
            val artistName: String,
            val currentPosition: Long,
            val duration: Long,
            val isPaused: Boolean
        ) : PlaybackUiState()
        data class Error(val message: String) : PlaybackUiState()
    }
    
    /**
     * Connect to Spotify for playback
     */
    fun connectToSpotify() {
        Log.d(TAG, "Connecting to Spotify for playback")
        spotifyPlaybackManager.connect()
    }
    
    /**
     * Disconnect from Spotify
     */
    fun disconnectFromSpotify() {
        Log.d(TAG, "Disconnecting from Spotify")
        spotifyPlaybackManager.disconnect()
    }
    
    /**
     * Play/pause the current track
     */
    fun togglePlayback() {
        Log.d(TAG, "Toggling playback")
        val currentState = _playbackState.value
        
        when (currentState) {
            is PlaybackUiState.Playing -> {
                if (currentState.isPaused) {
                    Log.d(TAG, "Resuming playback")
                    spotifyPlaybackManager.executeCommand(PlaybackCommand.Resume)
                } else {
                    Log.d(TAG, "Pausing playback")
                    spotifyPlaybackManager.executeCommand(PlaybackCommand.Pause)
                }
            }
            is PlaybackUiState.Connected -> {
                Log.d(TAG, "Starting playback")
                startCurrentSongPlayback()
            }
            else -> {
                Log.w(TAG, "Cannot toggle playback in current state: $currentState")
            }
        }
    }
    
    /**
     * Replay the current song from the beginning
     */
    fun replaySong() {
        Log.d(TAG, "Replaying current song")
        hasAutoStartedCurrentSong = false // Allow manual restart
        spotifyPlaybackManager.executeCommand(PlaybackCommand.Replay)
    }
    
    /**
     * Start playing the current session song
     */
    fun startCurrentSongPlayback() {
        Log.d(TAG, "Starting current song playback")
        currentSession?.currentSong?.let { song ->
            Log.d(TAG, "Playing song: ${song.title} by ${song.artist}")
            spotifyPlaybackManager.executeCommand(PlaybackCommand.PlayTrack("spotify:track:${song.spotifyId}"))
        } ?: run {
            Log.w(TAG, "No current song to play")
            _playbackState.value = PlaybackUiState.Error("No song selected to play")
        }
    }
    
    private fun onSpotifyConnected() {
        // Auto-start playback if we have a session in progress
        val session = currentSession
        if (session?.sessionState == FlashcardSessionState.IN_PROGRESS && session.currentSong != null) {
            startCurrentSongPlayback()
            hasAutoStartedCurrentSong = true
        }
    }
    
    private fun updatePlaybackUiState(playbackState: PlaybackState) {
        when {
            !playbackState.isConnected -> {
                _playbackState.value = PlaybackUiState.Disconnected
            }
            
            playbackState.error != null -> {
                _playbackState.value = PlaybackUiState.Error(playbackState.error.toString())
            }
            
            playbackState.trackName != null && playbackState.artistName != null -> {
                _playbackState.value = PlaybackUiState.Playing(
                    trackName = playbackState.trackName,
                    artistName = playbackState.artistName,
                    currentPosition = playbackState.positionMs,
                    duration = playbackState.durationMs,
                    isPaused = playbackState.isPaused
                )
            }
            
            else -> {
                _playbackState.value = PlaybackUiState.Connected
            }
        }
    }
    
    /**
     * Initialize the session by creating a new flashcard session
     */
    fun initializeSession() {
        Log.d(TAG, "Initializing flashcard session")
        
        // Validate user authentication
        val currentUser = authRepository.getCurrentUser()
        if (currentUser == null) {
            Log.e(TAG, "No authenticated user found")
            _sessionState.value = FlashcardSessionUiState.Error(
                "User not authenticated. Please log in again.",
                canRetry = false
            )
            return
        }
        
        Log.d(TAG, "User authenticated: ${currentUser.id}")
        
        _sessionState.value = FlashcardSessionUiState.Loading("Selecting songs for your session...")
        
        viewModelScope.launch {
            try {
                val sessionResult = createSessionUseCase(currentUser.id)
                
                if (sessionResult.isSuccess) {
                    val session = sessionResult.getOrNull()!!
                    currentSession = session
                    _sessionState.value = FlashcardSessionUiState.Ready(session)
                    Log.d(TAG, "Session initialized with ${session.totalSongs} songs")
                } else {
                    val error = sessionResult.exceptionOrNull()
                    Log.e(TAG, "Failed to create session", error)
                    
                    val (errorMessage, canRetry) = when (error) {
                        is InsufficientSongsException -> {
                            Pair(
                                "You need at least 3 songs in your library to start a session. " +
                                "Use 'Add Songs' to build your library first.",
                                false // Can't retry until user adds songs
                            )
                        }
                        else -> {
                            Pair(
                                "Failed to create session: ${error?.message ?: "Unknown error"}",
                                true // Can retry for other errors
                            )
                        }
                    }
                    
                    _sessionState.value = FlashcardSessionUiState.Error(errorMessage, canRetry)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception during session initialization", e)
                _sessionState.value = FlashcardSessionUiState.Error(
                    "An unexpected error occurred: ${e.message}",
                    canRetry = true
                )
            }
        }
    }
    
    /**
     * Start the actual flashcard session
     */
    fun startSession() {
        Log.d(TAG, "Starting flashcard session")
        
        val session = currentSession
        if (session == null) {
            Log.e(TAG, "Cannot start session - no session available")
            _sessionState.value = FlashcardSessionUiState.Error(
                "No session available to start",
                canRetry = true
            )
            return
        }
        
        // Validate session has songs
        if (session.songQueue.isEmpty()) {
            Log.e(TAG, "Cannot start session - no songs in queue")
            _sessionState.value = FlashcardSessionUiState.Error(
                "Cannot start session - no songs available",
                canRetry = true
            )
            return
        }
        
        // Validate session is in correct state to start
        if (session.sessionState != FlashcardSessionState.NOT_STARTED && 
            session.sessionState != FlashcardSessionState.PAUSED) {
            Log.w(TAG, "Session already started or completed")
            return
        }
        
        val startedSession = session.copy(
            sessionState = FlashcardSessionState.IN_PROGRESS,
            startedAt = Date()
        )
        
        currentSession = startedSession
        _sessionState.value = FlashcardSessionUiState.InProgress(startedSession)
        
        Log.d(TAG, "Session started with ${startedSession.totalSongs} songs")
        
        // Reset auto-start flag for new session
        hasAutoStartedCurrentSong = false
        
        // Auto-connect to Spotify and start playback
        connectToSpotify()
        // Playback will start automatically when Spotify connects
    }
    
    /**
     * Move to the next song in the session
     */
    fun moveToNextSong() {
        val session = currentSession
        if (session == null) {
            Log.e(TAG, "Cannot move to next song - no session available")
            _sessionState.value = FlashcardSessionUiState.Error(
                "No active session found",
                canRetry = false
            )
            return
        }
        
        // Validate session is in progress
        if (session.sessionState != FlashcardSessionState.IN_PROGRESS) {
            Log.w(TAG, "Cannot move to next song - session not in progress")
            return
        }
        
        Log.d(TAG, "Moving to next song. Current index: ${session.currentSongIndex}")
        
        val nextIndex = session.currentSongIndex + 1
        
        if (nextIndex >= session.totalSongs) {
            // Session completed
            val completedSession = session.copy(
                currentSongIndex = session.totalSongs,
                sessionState = FlashcardSessionState.COMPLETED,
                completedAt = Date()
            )
            
            currentSession = completedSession
            _sessionState.value = FlashcardSessionUiState.Completed(completedSession)
            
            Log.d(TAG, "Session completed!")
        } else {
            // Move to next song
            val updatedSession = session.copy(currentSongIndex = nextIndex)
            currentSession = updatedSession
            _sessionState.value = FlashcardSessionUiState.InProgress(updatedSession)
            
            Log.d(TAG, "Moved to song ${nextIndex + 1} of ${session.totalSongs}")
            
            // Reset auto-start flag for new song and start playing
            hasAutoStartedCurrentSong = false
            startCurrentSongPlayback()
            hasAutoStartedCurrentSong = true
        }
    }
    
    /**
     * Request to exit the session (will show confirmation if in progress)
     */
    fun requestSessionExit() {
        Log.d(TAG, "Exit requested")
        
        val session = currentSession
        
        when {
            session == null -> {
                // No session, just exit
                Log.d(TAG, "No session - exiting immediately")
                handleExitConfirmed()
            }
            
            session.sessionState == FlashcardSessionState.IN_PROGRESS -> {
                // Session in progress, need confirmation
                Log.d(TAG, "Session in progress - requesting confirmation")
                exitRequested = true
                // Fragment will show confirmation dialog
                // This is handled in the fragment by observing state changes
            }
            
            else -> {
                // Session not started or completed, can exit
                Log.d(TAG, "Session not in progress - exiting immediately")
                handleExitConfirmed()
            }
        }
    }
    
    /**
     * Confirm session exit (called from fragment after user confirms)
     */
    fun confirmSessionExit() {
        Log.d(TAG, "Session exit confirmed")
        handleExitConfirmed()
    }
    
    /**
     * Cancel session exit (called from fragment if user cancels)
     */
    fun cancelSessionExit() {
        Log.d(TAG, "Session exit cancelled")
        exitRequested = false
    }
    
    /**
     * Retry session creation after an error
     */
    fun retrySessionCreation() {
        Log.d(TAG, "Retrying session creation")
        initializeSession()
    }
    
    /**
     * Check if exit confirmation is needed
     */
    fun isExitConfirmationNeeded(): Boolean {
        return exitRequested && currentSession?.sessionState == FlashcardSessionState.IN_PROGRESS
    }
    
    private fun handleExitConfirmed() {
        Log.d(TAG, "Handling confirmed exit")
        
        // Stop Spotify playback and disconnect
        disconnectFromSpotify()
        
        // Mark session as cancelled if it was in progress
        currentSession?.let { session ->
            if (session.sessionState == FlashcardSessionState.IN_PROGRESS) {
                currentSession = session.copy(
                    sessionState = FlashcardSessionState.CANCELLED,
                    completedAt = Date()
                )
                Log.d(TAG, "Session marked as cancelled")
            }
        }
        
        // Clear state
        currentSession = null
        exitRequested = false
        hasAutoStartedCurrentSong = false
        
        // This should trigger navigation back to MainActivity
        // The Activity will handle finishing itself
    }
    
    override fun onCleared() {
        super.onCleared()
        // Ensure Spotify is disconnected when ViewModel is destroyed
        disconnectFromSpotify()
    }
    
    /**
     * Get the current session for state preservation
     */
    fun getCurrentSessionForSaving(): FlashcardSession? {
        return currentSession
    }
    
    /**
     * Restore session from saved state
     */
    fun restoreSession(session: FlashcardSession) {
        Log.d(TAG, "Restoring session from saved state")
        currentSession = session
        
        when (session.sessionState) {
            FlashcardSessionState.NOT_STARTED -> {
                _sessionState.value = FlashcardSessionUiState.Ready(session)
            }
            FlashcardSessionState.IN_PROGRESS -> {
                _sessionState.value = FlashcardSessionUiState.InProgress(session)
            }
            FlashcardSessionState.COMPLETED -> {
                _sessionState.value = FlashcardSessionUiState.Completed(session)
            }
            else -> {
                // CANCELLED or PAUSED - treat as ready to restart
                _sessionState.value = FlashcardSessionUiState.Ready(session)
            }
        }
    }
}