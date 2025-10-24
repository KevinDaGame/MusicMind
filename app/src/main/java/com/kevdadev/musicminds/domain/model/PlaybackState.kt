package com.kevdadev.musicminds.domain.model

/**
 * Represents the current playback state
 */
data class PlaybackState(
    val isPlaying: Boolean = false,
    val isPaused: Boolean = false,
    val trackUri: String? = null,
    val trackName: String? = null,
    val artistName: String? = null,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val isConnected: Boolean = false,
    val error: PlaybackError? = null
)

/**
 * Playback error types
 */
sealed class PlaybackError {
    object SpotifyNotInstalled : PlaybackError()
    object NotConnected : PlaybackError()
    object AuthenticationRequired : PlaybackError()
    object TrackUnavailable : PlaybackError()
    object NetworkError : PlaybackError()
    data class UnknownError(val message: String) : PlaybackError()
}

/**
 * Playback commands
 */
sealed class PlaybackCommand {
    data class PlayTrack(val spotifyUri: String) : PlaybackCommand()
    object Pause : PlaybackCommand()
    object Resume : PlaybackCommand()
    object Replay : PlaybackCommand()
    object Stop : PlaybackCommand()
}