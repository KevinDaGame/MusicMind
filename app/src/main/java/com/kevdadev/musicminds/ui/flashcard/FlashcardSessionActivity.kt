package com.kevdadev.musicminds.ui.flashcard

import android.os.Bundle
import android.util.Log
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.kevdadev.musicminds.R
import com.kevdadev.musicminds.domain.model.FlashcardSession
import kotlinx.coroutines.launch

/**
 * Activity for managing flashcard learning sessions
 */
class FlashcardSessionActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "FlashcardSessionActivity"
        private const val KEY_SESSION_STATE = "session_state"
    }
    
    private val viewModel: FlashcardSessionViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "FlashcardSessionActivity created")
        
        setContentView(R.layout.activity_flashcard_session)
        
        // Set up toolbar
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "Flashcard Session"
        }
        
        // Add the flashcard fragment
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, FlashcardSessionFragment.newInstance())
                .commit()
        }
        
        // Restore session state if available
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            savedInstanceState?.getParcelable(KEY_SESSION_STATE, FlashcardSession::class.java)?.let { session ->
                Log.d(TAG, "Restoring session from saved state")
                viewModel.restoreSession(session)
            }
        } else {
            @Suppress("DEPRECATION")
            savedInstanceState?.getParcelable<FlashcardSession>(KEY_SESSION_STATE)?.let { session ->
                Log.d(TAG, "Restoring session from saved state")
                viewModel.restoreSession(session)
            }
        }
        
        // Set up back button handling
        setupBackButtonHandling()
        
        // Observe session state for activity-level changes
        observeSessionState()
    }
    
    private fun observeSessionState() {
        lifecycleScope.launch {
            viewModel.sessionState.collect { state ->
                Log.d(TAG, "Session state changed to: $state")
                
                // Handle activity-level state changes
                when (state) {
                    is FlashcardSessionUiState.Completed -> {
                        // Session completed
                        Log.d(TAG, "Session completed")
                    }
                    is FlashcardSessionUiState.Error -> {
                        Log.e(TAG, "Session error: ${state.message}")
                        // For certain errors, might want to return to main
                        if (!state.canRetry) {
                            finish()
                        }
                    }
                    else -> {
                        // Other states are handled by fragment
                    }
                }
            }
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        // Handle back navigation with session exit confirmation
        viewModel.requestSessionExit()
        return true
    }
    
    private fun setupBackButtonHandling() {
        // Set up modern back button handling
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Handle system back button with session exit confirmation
                viewModel.requestSessionExit()
            }
        })
    }
    
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        
        // Save current session state
        viewModel.getCurrentSessionForSaving()?.let { session ->
            Log.d(TAG, "Saving session state")
            outState.putParcelable(KEY_SESSION_STATE, session)
        }
    }
}