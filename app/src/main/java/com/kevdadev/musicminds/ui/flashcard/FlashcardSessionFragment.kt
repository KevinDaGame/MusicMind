package com.kevdadev.musicminds.ui.flashcard

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.kevdadev.musicminds.R
import kotlinx.coroutines.launch

/**
 * Fragment for displaying the flashcard session UI
 */
class FlashcardSessionFragment : Fragment() {
    
    companion object {
        private const val TAG = "FlashcardSessionFragment"
        
        fun newInstance(): FlashcardSessionFragment {
            return FlashcardSessionFragment()
        }
    }
    
    private val viewModel: FlashcardSessionViewModel by activityViewModels()
    
    // UI components
    private lateinit var progressBar: ProgressBar
    private lateinit var progressText: TextView
    private lateinit var sessionStatusText: TextView
    private lateinit var currentSongInfo: TextView
    private lateinit var startSessionButton: Button
    private lateinit var exitSessionButton: Button
    private lateinit var revealAnswerButton: Button
    private lateinit var nextSongButton: Button
    
    // Playback UI components
    private lateinit var playbackControlsSection: View
    private lateinit var currentTrackName: TextView
    private lateinit var currentArtistName: TextView
    private lateinit var playbackProgressBar: ProgressBar
    private lateinit var currentPosition: TextView
    private lateinit var totalDuration: TextView
    private lateinit var replayButton: Button
    private lateinit var playPauseButton: Button
    private lateinit var playbackStatusText: TextView
    
    // Answer revelation UI components
    private lateinit var answerSection: View
    private lateinit var albumArtwork: android.widget.ImageView
    private lateinit var answerSongTitle: TextView
    private lateinit var answerArtistName: TextView
    private lateinit var answerReleaseYear: TextView
    private lateinit var answerAlbumName: TextView
    
    // Loading and error states
    private lateinit var loadingIndicator: ProgressBar
    private lateinit var errorText: TextView
    private lateinit var retryButton: Button
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_flashcard_session, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initViews(view)
        setupClickListeners()
        observeViewModel()
        
        // Start session creation process
        viewModel.initializeSession()
    }
    
    private fun initViews(view: View) {
        progressBar = view.findViewById(R.id.sessionProgressBar)
        progressText = view.findViewById(R.id.progressText)
        sessionStatusText = view.findViewById(R.id.sessionStatusText)
        currentSongInfo = view.findViewById(R.id.currentSongInfo)
        startSessionButton = view.findViewById(R.id.startSessionButton)
        exitSessionButton = view.findViewById(R.id.exitSessionButton)
        revealAnswerButton = view.findViewById(R.id.revealAnswerButton)
        nextSongButton = view.findViewById(R.id.nextSongButton)
        
        // Playback UI elements
        playbackControlsSection = view.findViewById(R.id.playbackControlsSection)
        currentTrackName = view.findViewById(R.id.currentTrackName)
        currentArtistName = view.findViewById(R.id.currentArtistName)
        playbackProgressBar = view.findViewById(R.id.playbackProgressBar)
        currentPosition = view.findViewById(R.id.currentPosition)
        totalDuration = view.findViewById(R.id.totalDuration)
        replayButton = view.findViewById(R.id.replayButton)
        playPauseButton = view.findViewById(R.id.playPauseButton)
        playbackStatusText = view.findViewById(R.id.playbackStatusText)
        
        // Answer revelation UI elements
        answerSection = view.findViewById(R.id.answerSection)
        albumArtwork = view.findViewById(R.id.albumArtwork)
        answerSongTitle = view.findViewById(R.id.answerSongTitle)
        answerArtistName = view.findViewById(R.id.answerArtistName)
        answerReleaseYear = view.findViewById(R.id.answerReleaseYear)
        answerAlbumName = view.findViewById(R.id.answerAlbumName)
        
        loadingIndicator = view.findViewById(R.id.loadingIndicator)
        errorText = view.findViewById(R.id.errorText)
        retryButton = view.findViewById(R.id.retryButton)
    }
    
    private fun setupClickListeners() {
        startSessionButton.setOnClickListener {
            viewModel.startSession()
        }
        
        exitSessionButton.setOnClickListener {
            handleExitRequest()
        }
        
        revealAnswerButton.setOnClickListener {
            viewModel.revealAnswer()
        }
        
        nextSongButton.setOnClickListener {
            viewModel.moveToNextSong()
        }
        
        retryButton.setOnClickListener {
            viewModel.retrySessionCreation()
        }
        
        // Playback control listeners
        playPauseButton.setOnClickListener {
            viewModel.togglePlayback()
        }
        
        // Long press to replay functionality
        playPauseButton.setOnLongClickListener {
            viewModel.replaySong()
            true // Consume the long press event
        }
    }
    
    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.sessionState.collect { state ->
                updateUI(state)
                
                // Check if exit confirmation is needed
                if (viewModel.isExitConfirmationNeeded()) {
                    showExitConfirmation {
                        viewModel.confirmSessionExit()
                        requireActivity().finish() // Return to MainActivity
                    }
                }
            }
        }
        
        // Observe playback state separately
        lifecycleScope.launch {
            viewModel.playbackState.collect { playbackState ->
                updatePlaybackUI(playbackState)
            }
        }
    }
    
    private fun updateUI(state: FlashcardSessionUiState) {
        Log.d(TAG, "Updating UI for state: ${state::class.java.simpleName}")
        
        when (state) {
            is FlashcardSessionUiState.Loading -> {
                showLoading(state.message)
            }
            
            is FlashcardSessionUiState.Ready -> {
                showReady(state)
            }
            
            is FlashcardSessionUiState.InProgress -> {
                showInProgress(state)
            }
            
            is FlashcardSessionUiState.AnswerRevealed -> {
                showAnswerRevealed(state)
            }
            
            is FlashcardSessionUiState.Completed -> {
                showCompleted(state)
            }
            
            is FlashcardSessionUiState.Error -> {
                showError(state)
            }
        }
    }
    
    private fun showLoading(message: String) {
        // Hide all other views
        setViewsVisibility(
            progressBar to false,
            progressText to false,
            sessionStatusText to false,
            currentSongInfo to false,
            startSessionButton to false,
            exitSessionButton to false,
            revealAnswerButton to false,
            nextSongButton to false,
            errorText to false,
            retryButton to false
        )
        
        // Show loading
        loadingIndicator.visibility = View.VISIBLE
        sessionStatusText.apply {
            visibility = View.VISIBLE
            text = message
        }
        
        // Hide answer section and playback controls
        answerSection.visibility = View.GONE
        playbackControlsSection.visibility = View.GONE
    }
    
    private fun showReady(state: FlashcardSessionUiState.Ready) {
        loadingIndicator.visibility = View.GONE
        
        setViewsVisibility(
            progressBar to true,
            progressText to true,
            sessionStatusText to true,
            startSessionButton to true,
            exitSessionButton to true,
            revealAnswerButton to false,
            nextSongButton to false,
            currentSongInfo to false,
            errorText to false,
            retryButton to false
        )
        
        // Update session info
        progressBar.max = state.session.totalSongs
        progressBar.progress = 0
        progressText.text = "Ready to start: ${state.session.totalSongs} songs selected"
        sessionStatusText.text = "Session ready! Tap Start to begin learning."
        
        // Hide answer section and playback controls
        answerSection.visibility = View.GONE
        playbackControlsSection.visibility = View.GONE
        
        Log.d(TAG, "Session ready with ${state.session.totalSongs} songs")
    }
    
    private fun showInProgress(state: FlashcardSessionUiState.InProgress) {
        loadingIndicator.visibility = View.GONE
        
        setViewsVisibility(
            progressBar to true,
            progressText to true,
            sessionStatusText to true,
            currentSongInfo to false, // Hide basic song info, show playback controls instead
            startSessionButton to false,
            exitSessionButton to true,
            revealAnswerButton to true, // Show reveal answer button during playback
            nextSongButton to false, // Hide next button until answer is revealed
            errorText to false,
            retryButton to false
        )
        
        // Show playback controls, hide answer section
        playbackControlsSection.visibility = View.VISIBLE
        playbackStatusText.visibility = View.VISIBLE
        answerSection.visibility = View.GONE
        
        // Update progress
        val session = state.session
        progressBar.progress = session.currentSongIndex + 1
        progressText.text = "Song ${session.currentSongIndex + 1} of ${session.totalSongs}"
        
        // Update current song info (no metadata revealed)
        session.currentSong?.let { song ->
            sessionStatusText.text = "🎵 Listen carefully and try to identify this song!"
            
            // DO NOT reveal song metadata - keep it hidden
            currentTrackName.text = "" // Keep empty to prevent leaks
            currentArtistName.text = "" // Keep empty to prevent leaks
            
            // Initialize playback progress
            playbackProgressBar.progress = 0
            currentPosition.text = "0:00"
            totalDuration.text = "0:00"
            
            playbackStatusText.text = "Ready to play • Tap to play/pause • Long press to replay"
        } ?: run {
            sessionStatusText.text = "Error: No song to display"
            playbackControlsSection.visibility = View.GONE
            playbackStatusText.text = "No song available"
        }
    }
    
    private fun showAnswerRevealed(state: FlashcardSessionUiState.AnswerRevealed) {
        loadingIndicator.visibility = View.GONE
        
        setViewsVisibility(
            progressBar to true,
            progressText to true,
            sessionStatusText to true,
            currentSongInfo to false, // Hide basic song info, show answer section instead
            startSessionButton to false,
            exitSessionButton to true,
            revealAnswerButton to false, // Hide reveal button after revealing
            nextSongButton to true, // Show next button
            errorText to false,
            retryButton to false
        )
        
        // Show playback controls and answer section
        playbackControlsSection.visibility = View.VISIBLE
        playbackStatusText.visibility = View.VISIBLE
        answerSection.visibility = View.VISIBLE
        
        // Update progress
        val session = state.session
        progressBar.progress = session.currentSongIndex + 1
        progressText.text = "Song ${session.currentSongIndex + 1} of ${session.totalSongs}"
        
        // Update session status
        sessionStatusText.text = "Answer revealed! Ready for the next song?"
        
        // Display answer information
        session.currentSong?.let { song ->
            answerSongTitle.text = song.title
            answerArtistName.text = song.artist
            answerReleaseYear.text = "Released: ${song.releaseYear}"
            answerAlbumName.text = song.album
            
            // Load album artwork with Glide
            if (!song.imageUrl.isNullOrBlank()) {
                Glide.with(this)
                    .load(song.imageUrl)
                    .apply(RequestOptions().transform(RoundedCorners(16)))
                    .placeholder(R.drawable.ic_launcher_foreground) // Use app icon as placeholder
                    .error(android.R.drawable.ic_menu_gallery) // Default image on error
                    .into(albumArtwork)
            } else {
                // Use default image if no artwork URL
                albumArtwork.setImageResource(android.R.drawable.ic_menu_gallery)
            }
            
            Log.d(TAG, "Answer revealed for: ${song.title} by ${song.artist} (${song.releaseYear})")
        } ?: run {
            answerSongTitle.text = "Unknown Song"
            answerArtistName.text = "Unknown Artist"
            answerReleaseYear.text = "Unknown Year"
            answerAlbumName.text = "Unknown Album"
            albumArtwork.setImageResource(android.R.drawable.ic_menu_gallery)
        }
        
        // Update next button text based on if this is the last song
        val isLastSong = session.currentSongIndex >= session.totalSongs - 1
        nextSongButton.text = if (isLastSong) "Finish Session" else "Next Song"
    }
    
    private fun updatePlaybackUI(playbackState: FlashcardSessionViewModel.PlaybackUiState) {
        Log.d(TAG, "Updating playback UI for state: ${playbackState::class.java.simpleName}")
        
        when (playbackState) {
            is FlashcardSessionViewModel.PlaybackUiState.Disconnected -> {
                playbackStatusText.text = "Connecting to Spotify..."
                playPauseButton.isEnabled = false
                playPauseButton.text = "▶"
                playbackProgressBar.progress = 0
                currentPosition.text = "0:00"
                totalDuration.text = "0:00"
                
                // Do NOT reveal song information
                currentTrackName.text = ""
                currentArtistName.text = ""
            }
            
            is FlashcardSessionViewModel.PlaybackUiState.Connected -> {
                playbackStatusText.text = "Ready to play • Tap to play • Long press to replay"
                playPauseButton.isEnabled = true
                playPauseButton.text = "▶"
                
                // Do NOT reveal song information
                currentTrackName.text = ""
                currentArtistName.text = ""
            }
            
            is FlashcardSessionViewModel.PlaybackUiState.Playing -> {
                playbackStatusText.text = if (playbackState.isPaused) "⏸ Paused" else "🎵 Playing"
                playPauseButton.isEnabled = true
                
                // Update button text based on playback state
                playPauseButton.text = if (playbackState.isPaused) "▶" else "⏸"
                
                // DO NOT reveal track information during playback - keep it hidden
                currentTrackName.text = ""
                currentArtistName.text = ""
                
                // Update progress
                val progressPercent = if (playbackState.duration > 0) {
                    ((playbackState.currentPosition.toFloat() / playbackState.duration) * 100).toInt()
                } else 0
                playbackProgressBar.progress = progressPercent
                
                currentPosition.text = formatTime(playbackState.currentPosition)
                totalDuration.text = formatTime(playbackState.duration)
                
                // Update play/pause button icon
                // You can add icon changes here if you have drawable resources
            }
            
            is FlashcardSessionViewModel.PlaybackUiState.Error -> {
                playbackStatusText.text = "Playback error: ${playbackState.message}"
                playPauseButton.isEnabled = false
                playPauseButton.text = "▶"
            }
        }
    }
    
    private fun formatTime(milliseconds: Long): String {
        val seconds = (milliseconds / 1000) % 60
        val minutes = (milliseconds / (1000 * 60)) % 60
        return String.format("%d:%02d", minutes, seconds)
    }

    private fun showCompleted(state: FlashcardSessionUiState.Completed) {
        loadingIndicator.visibility = View.GONE
        
        setViewsVisibility(
            progressBar to true,
            progressText to true,
            sessionStatusText to true,
            currentSongInfo to true,
            startSessionButton to false,
            exitSessionButton to true,
            revealAnswerButton to false,
            nextSongButton to false,
            errorText to false,
            retryButton to false
        )
        
        val session = state.session
        progressBar.progress = session.totalSongs
        progressText.text = "Session Complete!"
        
        // Create a comprehensive session summary
        val sessionDuration = if (session.startedAt != null && session.completedAt != null) {
            val duration = session.completedAt.time - session.startedAt.time
            val minutes = duration / (1000 * 60)
            " in ${minutes} minutes"
        } else {
            ""
        }
        
        sessionStatusText.text = "🎉 Congratulations! 🎉"
        currentSongInfo.text = "You completed all ${session.totalSongs} songs${sessionDuration}!\n\n" +
                "Thanks for practicing with MusicMinds. Your musical knowledge is growing!"
        
        // Hide answer section and playback controls in completed state
        answerSection.visibility = View.GONE
        playbackControlsSection.visibility = View.GONE
        
        // Update exit button text
        exitSessionButton.text = "Return to Main"
    }
    
    private fun showError(state: FlashcardSessionUiState.Error) {
        loadingIndicator.visibility = View.GONE
        
        setViewsVisibility(
            progressBar to false,
            progressText to false,
            sessionStatusText to true,
            currentSongInfo to false,
            startSessionButton to false,
            exitSessionButton to true,
            revealAnswerButton to false,
            nextSongButton to false,
            errorText to true,
            retryButton to state.canRetry
        )
        
        sessionStatusText.text = if (state.canRetry) "Session Error" else "Cannot Start Session"
        errorText.text = state.message
        
        // Hide answer section and playback controls in error state
        answerSection.visibility = View.GONE
        playbackControlsSection.visibility = View.GONE
        
        // Update exit button text based on error type
        exitSessionButton.text = if (state.canRetry) "Exit" else "Back to Main"
        
        Log.e(TAG, "Session error: ${state.message}, canRetry: ${state.canRetry}")
    }
    
    private fun setViewsVisibility(vararg viewsAndVisibility: Pair<View, Boolean>) {
        viewsAndVisibility.forEach { (view, visible) ->
            view.visibility = if (visible) View.VISIBLE else View.GONE
        }
    }
    
    fun showExitConfirmation(onConfirm: () -> Unit) {
        AlertDialog.Builder(requireContext())
            .setTitle("Exit Session")
            .setMessage("Are you sure you want to exit the current session? Your progress will be lost.")
            .setPositiveButton("Exit") { _, _ -> onConfirm() }
            .setNegativeButton("Continue") { _, _ ->
                viewModel.cancelSessionExit()
            }
            .show()
    }
    
    private fun handleExitRequest() {
        val session = viewModel.getCurrentSessionForSaving()
        
        if (session?.sessionState == com.kevdadev.musicminds.domain.model.FlashcardSessionState.IN_PROGRESS) {
            // Show confirmation for active session
            showExitConfirmation {
                viewModel.confirmSessionExit()
                requireActivity().finish()
            }
        } else {
            // Can exit immediately if session not in progress
            viewModel.confirmSessionExit()
            requireActivity().finish()
        }
    }
}