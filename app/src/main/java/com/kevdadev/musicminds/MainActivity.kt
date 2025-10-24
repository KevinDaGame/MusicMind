package com.kevdadev.musicminds

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.kevdadev.musicminds.auth.AuthRepository
import com.kevdadev.musicminds.auth.data.AuthState
import com.kevdadev.musicminds.ui.auth.AuthActivity
import com.spotify.android.appremote.api.ConnectionParams
import com.spotify.android.appremote.api.Connector
import com.spotify.android.appremote.api.SpotifyAppRemote
import com.spotify.protocol.client.Subscription
import com.spotify.protocol.types.PlayerState
import com.spotify.protocol.types.Track
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    companion object {
        private const val CLIENT_ID = "0897452c456b4f468a7d1ca8bc42f535" // Replace with your actual client ID
        private const val REDIRECT_URI = "musicminds://callback"
        private const val TAG = "MainActivity"
    }

    private var spotifyAppRemote: SpotifyAppRemote? = null
    private lateinit var connectionStatusTextView: TextView
    private lateinit var trackInfoTextView: TextView
    private var connectionTimeoutHandler: Handler? = null
    private var isConnecting = false
    private lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate called")
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        
        // Initialize auth repository
        authRepository = AuthRepository.getInstance(application)
        
        try {
            // Initialize UI elements
            connectionStatusTextView = findViewById(R.id.connection_status)
            trackInfoTextView = findViewById(R.id.track_info)
            
            // Initialize Add Songs button
            val addSongsButton = findViewById<android.widget.Button>(R.id.addSongsButton)
            addSongsButton.setOnClickListener {
                val intent = Intent(this, com.kevdadev.musicminds.ui.search.SearchActivity::class.java)
                startActivity(intent)
            }
            
            // Initialize Library button
            val libraryButton = findViewById<android.widget.Button>(R.id.libraryButton)
            libraryButton.setOnClickListener {
                val intent = Intent(this, com.kevdadev.musicminds.ui.library.LibraryActivity::class.java)
                startActivity(intent)
            }
            
            // Initialize Start Flashcard Session button
            val startFlashcardButton = findViewById<android.widget.Button>(R.id.startFlashcardButton)
            startFlashcardButton.setOnClickListener {
                val intent = Intent(this, com.kevdadev.musicminds.ui.flashcard.FlashcardSessionActivity::class.java)
                startActivity(intent)
            }
            
            Log.d(TAG, "UI elements initialized successfully")
            
            // Check authentication status
            checkAuthenticationStatus()
            
            // Run diagnostics
            logSystemDiagnostics()
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing UI elements", e)
        }
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
    
    private fun checkAuthenticationStatus() {
        lifecycleScope.launch {
            authRepository.authState.collect { state ->
                when (state) {
                    is AuthState.Unauthenticated, is AuthState.AuthError -> {
                        Log.d(TAG, "User not authenticated, redirecting to auth")
                        redirectToAuth()
                        return@collect
                    }
                    is AuthState.Authenticated -> {
                        Log.d(TAG, "User authenticated, proceeding with Spotify connection")
                        // User is authenticated, continue with normal flow
                    }
                    is AuthState.Authenticating, 
                    is AuthState.RefreshingToken, is AuthState.TokenExpired -> {
                        Log.d(TAG, "Authentication status loading...")
                    }
                }
            }
        }
    }
    
    private fun redirectToAuth() {
        val intent = Intent(this, AuthActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart called - attempting Spotify connection")
        Log.d(TAG, "Client ID: $CLIENT_ID")
        Log.d(TAG, "Redirect URI: $REDIRECT_URI")
        
        // Check if Spotify is installed
        try {
            val pm = packageManager
            val spotifyPackage = pm.getPackageInfo("com.spotify.music", 0)
            Log.d(TAG, "Spotify app is installed - version: ${spotifyPackage.versionName}")
        } catch (e: Exception) {
            Log.e(TAG, "Spotify app not found", e)
            connectionStatusTextView.text = "Spotify app not installed"
            return
        }
        
        try {
            connectionStatusTextView.text = "Connecting to Spotify..."
            Log.d(TAG, "UI updated: Connecting to Spotify...")
            
            // Try first without auth view to see if that's the issue
            val connectionParams = ConnectionParams.Builder(CLIENT_ID)
                .setRedirectUri(REDIRECT_URI)
                .showAuthView(false) // Try without auth view first
                .build()
            
            Log.d(TAG, "Connection parameters built successfully (no auth view)")
            
            // Set up timeout handler
            isConnecting = true
            connectionTimeoutHandler = Handler(Looper.getMainLooper())
            connectionTimeoutHandler?.postDelayed({
                if (isConnecting) {
                    Log.w(TAG, "Connection timeout after 30 seconds - trying with auth view")
                    isConnecting = false
                    tryConnectionWithAuth()
                }
            }, 30000) // 30 second timeout

            Log.d(TAG, "About to call SpotifyAppRemote.connect...")
            SpotifyAppRemote.connect(this, connectionParams, object : Connector.ConnectionListener {
            override fun onConnected(appRemote: SpotifyAppRemote) {
                isConnecting = false
                connectionTimeoutHandler?.removeCallbacksAndMessages(null)
                spotifyAppRemote = appRemote
                Log.d(TAG, "Connected! Yay!")
                runOnUiThread {
                    connectionStatusTextView.text = "Connected to Spotify!"
                }
                // Now you can start interacting with App Remote
                connected()
            }

            override fun onFailure(throwable: Throwable) {
                isConnecting = false
                connectionTimeoutHandler?.removeCallbacksAndMessages(null)
                Log.e(TAG, "Connection failed: ${throwable.message}", throwable)
                Log.e(TAG, "Exception type: ${throwable::class.java.simpleName}")
                Log.e(TAG, "Stack trace: ${throwable.stackTraceToString()}")
                
                runOnUiThread {
                    connectionStatusTextView.text = "Failed to connect: ${throwable.message}"
                }
                
                // Log specific error types
                when (throwable::class.java.simpleName) {
                    "UserNotAuthorizedException" -> {
                        Log.d(TAG, "User not authorized - automatically trying with auth view")
                        runOnUiThread {
                            connectionStatusTextView.text = "Authorization required - trying with auth..."
                        }
                        // Automatically try with auth view
                        tryConnectionWithAuth()
                        return
                    }
                    "SpotifyDisconnectedException" -> {
                        Log.e(TAG, "Spotify app not available or not logged in")
                        runOnUiThread {
                            connectionStatusTextView.text = "Please open and login to Spotify app"
                        }
                    }
                    "CouldNotFindSpotifyApp" -> {
                        Log.e(TAG, "Spotify app not installed")
                        runOnUiThread {
                            connectionStatusTextView.text = "Please install Spotify app"
                        }
                    }
                    else -> {
                        Log.e(TAG, "Other connection error: ${throwable.message}")
                        runOnUiThread {
                            connectionStatusTextView.text = "Connection error: ${throwable::class.java.simpleName}"
                        }
                    }
                }
            }
        })
        } catch (e: Exception) {
            Log.e(TAG, "Exception during connection setup", e)
            connectionStatusTextView.text = "Setup error: ${e.message}"
        }
    }
    
    private fun tryConnectionWithAuth() {
        Log.d(TAG, "Attempting connection with auth view...")
        runOnUiThread {
            connectionStatusTextView.text = "Trying with authentication..."
        }
        
        try {
            val connectionParams = ConnectionParams.Builder(CLIENT_ID)
                .setRedirectUri(REDIRECT_URI)
                .showAuthView(true)
                .build()
            
            Log.d(TAG, "Auth connection parameters built successfully")
            
            // Set up timeout handler for auth attempt
            isConnecting = true
            connectionTimeoutHandler = Handler(Looper.getMainLooper())
            connectionTimeoutHandler?.postDelayed({
                if (isConnecting) {
                    Log.w(TAG, "Auth connection timeout after 30 seconds")
                    runOnUiThread {
                        connectionStatusTextView.text = "Connection failed - check dashboard setup"
                    }
                    isConnecting = false
                }
            }, 30000)

            SpotifyAppRemote.connect(this, connectionParams, object : Connector.ConnectionListener {
                override fun onConnected(appRemote: SpotifyAppRemote) {
                    isConnecting = false
                    connectionTimeoutHandler?.removeCallbacksAndMessages(null)
                    spotifyAppRemote = appRemote
                    Log.d(TAG, "Connected with auth! Yay!")
                    runOnUiThread {
                        connectionStatusTextView.text = "Connected to Spotify!"
                    }
                    connected()
                }

                override fun onFailure(throwable: Throwable) {
                    isConnecting = false
                    connectionTimeoutHandler?.removeCallbacksAndMessages(null)
                    Log.e(TAG, "Auth connection failed: ${throwable.message}", throwable)
                    Log.e(TAG, "Auth exception type: ${throwable::class.java.simpleName}")
                    Log.e(TAG, "Auth stack trace: ${throwable.stackTraceToString()}")
                    
                    runOnUiThread {
                        connectionStatusTextView.text = "Auth failed: ${throwable::class.java.simpleName}"
                    }
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Exception during auth connection setup", e)
            runOnUiThread {
                connectionStatusTextView.text = "Auth setup error: ${e.message}"
            }
        }
    }

    private fun connected() {
        spotifyAppRemote?.let {
            // Connected to Spotify - ready for user interactions
            Log.d(TAG, "Spotify connection established - ready for user commands")
            
            // Update UI to show ready state
            runOnUiThread {
                trackInfoTextView.text = "Connected to Spotify - ready to play music!"
            }
        }
    }

    override fun onStop() {
        super.onStop()
        spotifyAppRemote?.let {
            SpotifyAppRemote.disconnect(it)
        }
        connectionStatusTextView.text = "Disconnected from Spotify"
        trackInfoTextView.text = "No track playing"
    }
    
    private fun logSystemDiagnostics() {
        Log.d(TAG, "=== System Diagnostics ===")
        Log.d(TAG, "Android version: ${android.os.Build.VERSION.RELEASE}")
        Log.d(TAG, "Device: ${android.os.Build.MODEL}")
        Log.d(TAG, "Package name: ${packageName}")
        
        // Check internet connectivity
        val cm = getSystemService(CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val network = cm.activeNetwork
        val capabilities = cm.getNetworkCapabilities(network)
        val hasInternet = capabilities?.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET) ?: false
        Log.d(TAG, "Internet connectivity: $hasInternet")
        
        // Log all installed packages containing 'spotify'
        val packages = packageManager.getInstalledPackages(0)
        val spotifyPackages = packages.filter { it.packageName.contains("spotify", ignoreCase = true) }
        Log.d(TAG, "Spotify-related packages found: ${spotifyPackages.size}")
        spotifyPackages.forEach { 
            Log.d(TAG, "Package: ${it.packageName}, Version: ${it.versionName}")
        }
        Log.d(TAG, "=== End Diagnostics ===")
    }
}