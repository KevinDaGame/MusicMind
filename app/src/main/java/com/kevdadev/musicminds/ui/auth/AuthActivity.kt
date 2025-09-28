package com.kevdadev.musicminds.ui.auth

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.kevdadev.musicminds.MainActivity
import com.kevdadev.musicminds.R
import com.kevdadev.musicminds.auth.data.AuthState
import com.spotify.sdk.android.auth.AuthorizationClient
import kotlinx.coroutines.launch

/**
 * Activity that handles Spotify authentication
 */
class AuthActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "AuthActivity"
        private const val REQUEST_CODE = 1337
    }
    
    private lateinit var viewModel: AuthViewModel
    
    // UI components
    private lateinit var connectButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var statusText: TextView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)
        
        viewModel = ViewModelProvider(this)[AuthViewModel::class.java]
        
        initViews()
        observeAuthState()
    }
    
    private fun initViews() {
        connectButton = findViewById(R.id.btn_connect_spotify)
        progressBar = findViewById(R.id.progress_bar)
        statusText = findViewById(R.id.tv_status)
        
        connectButton.setOnClickListener {
            authenticateSpotify()
        }
    }
    
    private fun observeAuthState() {
        lifecycleScope.launch {
            viewModel.authState.collect { state ->
                when (state) {
                    is AuthState.Authenticating -> {
                        showLoading(true)
                        statusText.text = "Connecting to Spotify..."
                        Log.d(TAG, "Authentication in progress")
                    }
                    
                    is AuthState.Authenticated -> {
                        showLoading(false)
                        statusText.text = "Connected successfully!"
                        Log.d(TAG, "Authentication successful for user: ${state.userInfo.displayName}")
                        navigateToMain()
                    }
                    
                    is AuthState.Unauthenticated -> {
                        showLoading(false)
                        statusText.text = "Connect your Spotify account to continue"
                        connectButton.isEnabled = true
                        Log.d(TAG, "User not authenticated")
                    }
                    
                    is AuthState.AuthError -> {
                        showLoading(false)
                        statusText.text = "Connection failed: ${state.error}"
                        connectButton.isEnabled = true
                        Log.e(TAG, "Authentication error: ${state.error}")
                    }
                    
                    is AuthState.TokenExpired, is AuthState.RefreshingToken -> {
                        showLoading(true)
                        statusText.text = "Refreshing connection..."
                        Log.d(TAG, "Token refresh in progress")
                    }
                }
            }
        }
    }
    
    private fun authenticateSpotify() {
        Log.d(TAG, "Starting Spotify authentication")
        val authRequest = viewModel.createAuthRequest()
        AuthorizationClient.openLoginActivity(this, REQUEST_CODE, authRequest)
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == REQUEST_CODE) {
            Log.d(TAG, "Received auth result - requestCode: $requestCode, resultCode: $resultCode")
            viewModel.handleAuthResult(requestCode, resultCode, data)
        }
    }
    
    private fun showLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        connectButton.isEnabled = !isLoading
    }
    
    private fun navigateToMain() {
        Log.d(TAG, "Navigating to MainActivity")
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}