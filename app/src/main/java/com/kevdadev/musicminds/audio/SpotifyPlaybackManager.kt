package com.kevdadev.musicminds.audio

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.kevdadev.musicminds.auth.data.AuthConstants
import com.kevdadev.musicminds.domain.model.PlaybackCommand
import com.kevdadev.musicminds.domain.model.PlaybackError
import com.kevdadev.musicminds.domain.model.PlaybackState
import com.spotify.android.appremote.api.ConnectionParams
import com.spotify.android.appremote.api.Connector
import com.spotify.android.appremote.api.SpotifyAppRemote
import com.spotify.protocol.client.CallResult
import com.spotify.protocol.client.Subscription
import com.spotify.protocol.types.PlayerState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages Spotify playback for flashcard sessions
 */
class SpotifyPlaybackManager(private val context: Context) {
    
    companion object {
        private const val TAG = "SpotifyPlaybackManager"
    }
    
    private var spotifyAppRemote: SpotifyAppRemote? = null
    // Note: Spotify SDK subscriptions are automatically managed by the SDK
    private var isConnecting = false
    
    // Timer for position updates
    private val handler = Handler(Looper.getMainLooper())
    private var positionUpdateRunnable: Runnable? = null
    private var lastKnownPosition = 0L
    private var lastUpdateTime = 0L
    
    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()
    
    /**
     * Connect to Spotify App Remote
     */
    fun connect(): Boolean {
        if (spotifyAppRemote?.isConnected == true) {
            Log.d(TAG, "Already connected to Spotify")
            return true
        }
        
        if (isConnecting) {
            Log.d(TAG, "Connection already in progress")
            return false
        }
        
        Log.d(TAG, "Connecting to Spotify App Remote")
        isConnecting = true
        
        val connectionParams = ConnectionParams.Builder(AuthConstants.CLIENT_ID)
            .setRedirectUri(AuthConstants.REDIRECT_URI)
            .showAuthView(false) // Don't show auth view - assume already authenticated
            .build()
        
        SpotifyAppRemote.connect(context, connectionParams, object : Connector.ConnectionListener {
            override fun onConnected(appRemote: SpotifyAppRemote) {
                isConnecting = false
                spotifyAppRemote = appRemote
                Log.d(TAG, "Connected to Spotify App Remote")
                
                // Subscribe to player state changes
                subscribeToPlayerState()
                
                // Update connection state
                _playbackState.value = _playbackState.value.copy(
                    isConnected = true,
                    error = null
                )
            }
            
            override fun onFailure(throwable: Throwable) {
                isConnecting = false
                Log.e(TAG, "Failed to connect to Spotify", throwable)
                
                val error = when (throwable::class.java.simpleName) {
                    "CouldNotFindSpotifyApp" -> PlaybackError.SpotifyNotInstalled
                    "UserNotAuthorizedException" -> PlaybackError.AuthenticationRequired
                    "SpotifyDisconnectedException" -> PlaybackError.NotConnected
                    else -> PlaybackError.UnknownError(throwable.message ?: "Unknown connection error")
                }
                
                _playbackState.value = _playbackState.value.copy(
                    isConnected = false,
                    error = error
                )
            }
        })
        
        return true
    }
    
    /**
     * Start periodic position updates for smooth progress bar
     */
    private fun startPositionUpdates() {
        stopPositionUpdates() // Ensure no duplicate timers
        
        positionUpdateRunnable = object : Runnable {
            override fun run() {
                val currentState = _playbackState.value
                if (currentState.isPlaying && !currentState.isPaused) {
                    val now = System.currentTimeMillis()
                    val elapsed = now - lastUpdateTime
                    val estimatedPosition = lastKnownPosition + elapsed
                    
                    // Only update if we have valid duration and position
                    if (currentState.durationMs > 0 && estimatedPosition <= currentState.durationMs) {
                        val updatedState = currentState.copy(positionMs = estimatedPosition)
                        _playbackState.value = updatedState
                    }
                }
                
                // Schedule next update in 100ms for smooth progress
                handler.postDelayed(this, 100)
            }
        }
        
        positionUpdateRunnable?.let { handler.post(it) }
    }
    
    /**
     * Stop periodic position updates
     */
    private fun stopPositionUpdates() {
        positionUpdateRunnable?.let {
            handler.removeCallbacks(it)
            positionUpdateRunnable = null
        }
    }
    
    /**
     * Disconnect from Spotify App Remote
     */
    fun disconnect() {
        Log.d(TAG, "Disconnecting from Spotify")
        
        // Stop position updates
        stopPositionUpdates()
        
        // Spotify SDK automatically manages subscriptions when disconnecting
        
        spotifyAppRemote?.let {
            SpotifyAppRemote.disconnect(it)
        }
        spotifyAppRemote = null
        
        _playbackState.value = PlaybackState(isConnected = false)
    }
    
    /**
     * Execute a playback command
     */
    fun executeCommand(command: PlaybackCommand) {
        val appRemote = spotifyAppRemote
        if (appRemote == null || !appRemote.isConnected) {
            Log.w(TAG, "Cannot execute command - not connected to Spotify")
            _playbackState.value = _playbackState.value.copy(error = PlaybackError.NotConnected)
            return
        }
        
        Log.d(TAG, "Executing playback command: $command")
        
        when (command) {
            is PlaybackCommand.PlayTrack -> {
                playTrack(appRemote, command.spotifyUri)
            }
            is PlaybackCommand.Pause -> {
                pausePlayback(appRemote)
            }
            is PlaybackCommand.Resume -> {
                resumePlayback(appRemote)
            }
            is PlaybackCommand.Replay -> {
                replayTrack(appRemote)
            }
            is PlaybackCommand.Stop -> {
                stopPlayback(appRemote)
            }
        }
    }
    
    private fun playTrack(appRemote: SpotifyAppRemote, spotifyUri: String) {
        Log.d(TAG, "Playing track: $spotifyUri")
        
        appRemote.playerApi.play(spotifyUri)
            .setResultCallback { 
                Log.d(TAG, "Successfully started playing track")
                // Clear any previous errors
                _playbackState.value = _playbackState.value.copy(error = null)
            }
            .setErrorCallback { throwable ->
                Log.e(TAG, "Failed to play track", throwable)
                val error = when {
                    throwable.message?.contains("track not found", ignoreCase = true) == true -> {
                        PlaybackError.TrackUnavailable
                    }
                    throwable.message?.contains("network", ignoreCase = true) == true -> {
                        PlaybackError.NetworkError
                    }
                    else -> PlaybackError.UnknownError(throwable.message ?: "Playback failed")
                }
                _playbackState.value = _playbackState.value.copy(error = error)
            }
    }
    
    private fun pausePlayback(appRemote: SpotifyAppRemote) {
        Log.d(TAG, "Pausing playback")
        
        appRemote.playerApi.pause()
            .setResultCallback { 
                Log.d(TAG, "Successfully paused playback")
            }
            .setErrorCallback { throwable ->
                Log.e(TAG, "Failed to pause playback", throwable)
                _playbackState.value = _playbackState.value.copy(
                    error = PlaybackError.UnknownError(throwable.message ?: "Failed to pause")
                )
            }
    }
    
    private fun resumePlayback(appRemote: SpotifyAppRemote) {
        Log.d(TAG, "Resuming playback")
        
        appRemote.playerApi.resume()
            .setResultCallback { 
                Log.d(TAG, "Successfully resumed playback")
            }
            .setErrorCallback { throwable ->
                Log.e(TAG, "Failed to resume playback", throwable)
                _playbackState.value = _playbackState.value.copy(
                    error = PlaybackError.UnknownError(throwable.message ?: "Failed to resume")
                )
            }
    }
    
    private fun replayTrack(appRemote: SpotifyAppRemote) {
        Log.d(TAG, "Replaying current track")
        
        // Seek to beginning and play
        appRemote.playerApi.seekTo(0)
            .setResultCallback {
                Log.d(TAG, "Successfully seeked to beginning")
                // Resume playing if paused
                if (_playbackState.value.isPaused) {
                    resumePlayback(appRemote)
                }
            }
            .setErrorCallback { throwable ->
                Log.e(TAG, "Failed to replay track", throwable)
                _playbackState.value = _playbackState.value.copy(
                    error = PlaybackError.UnknownError(throwable.message ?: "Failed to replay")
                )
            }
    }
    
    private fun stopPlayback(appRemote: SpotifyAppRemote) {
        Log.d(TAG, "Stopping playback")
        
        appRemote.playerApi.pause()
            .setResultCallback { 
                Log.d(TAG, "Successfully stopped playback")
            }
            .setErrorCallback { throwable ->
                Log.e(TAG, "Failed to stop playback", throwable)
                _playbackState.value = _playbackState.value.copy(
                    error = PlaybackError.UnknownError(throwable.message ?: "Failed to stop")
                )
            }
    }
    
    private fun subscribeToPlayerState() {
        val appRemote = spotifyAppRemote ?: return
        
        Log.d(TAG, "Subscribing to player state changes")
        
        appRemote.playerApi.subscribeToPlayerState()
            .setEventCallback { playerState ->
                Log.d(TAG, "Player state changed: playing=${!playerState.isPaused}, track=${playerState.track?.name}")
                
                val track = playerState.track
                val isPlaying = !playerState.isPaused
                
                // Update known position and time for smooth interpolation
                lastKnownPosition = playerState.playbackPosition
                lastUpdateTime = System.currentTimeMillis()
                
                val newState = _playbackState.value.copy(
                    isPlaying = isPlaying,
                    isPaused = playerState.isPaused,
                    trackUri = track?.uri,
                    trackName = track?.name,
                    artistName = track?.artist?.name,
                    positionMs = playerState.playbackPosition,
                    durationMs = track?.duration ?: 0,
                    error = null // Clear errors on successful state update
                )
                _playbackState.value = newState
                
                // Start/stop position updates based on playing state
                if (isPlaying && track != null) {
                    startPositionUpdates()
                } else {
                    stopPositionUpdates()
                }
            }
            .setErrorCallback { throwable ->
                Log.e(TAG, "Error in player state subscription", throwable)
                _playbackState.value = _playbackState.value.copy(
                    error = PlaybackError.UnknownError("Player state error: ${throwable.message}")
                )
            }
    }
    
    /**
     * Check if currently connected to Spotify
     */
    fun isConnected(): Boolean {
        return spotifyAppRemote?.isConnected == true
    }
    
    /**
     * Get current track URI if playing
     */
    fun getCurrentTrackUri(): String? {
        return _playbackState.value.trackUri
    }
}