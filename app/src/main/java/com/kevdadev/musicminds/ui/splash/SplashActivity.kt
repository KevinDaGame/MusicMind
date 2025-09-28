package com.kevdadev.musicminds.ui.splash

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.kevdadev.musicminds.R
import com.kevdadev.musicminds.auth.data.AuthState
import com.kevdadev.musicminds.ui.auth.AuthActivity
import com.kevdadev.musicminds.MainActivity
import kotlinx.coroutines.launch

/**
 * Splash screen that checks authentication status and navigates appropriately
 */
class SplashActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "SplashActivity"
    }
    
    private lateinit var viewModel: SplashViewModel
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        
        Log.d(TAG, "SplashActivity created")
        
        viewModel = ViewModelProvider(this)[SplashViewModel::class.java]
        
        observeAuthState()
    }
    
    private fun observeAuthState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.authState.collect { state ->
                    Log.d(TAG, "Auth state changed: ${state::class.java.simpleName}")
                    handleAuthState(state)
                }
            }
        }
    }
    
    private fun handleAuthState(state: AuthState) {
        when (state) {
            is AuthState.Authenticated -> {
                Log.d(TAG, "User is authenticated, navigating to MainActivity")
                navigateToMain()
            }
            
            is AuthState.Unauthenticated,
            is AuthState.TokenExpired -> {
                Log.d(TAG, "User needs authentication, navigating to AuthActivity")
                navigateToAuth()
            }
            
            is AuthState.AuthError -> {
                Log.e(TAG, "Authentication error: ${state.error}")
                // For now, navigate to auth activity to retry
                navigateToAuth()
            }
            
            is AuthState.Authenticating,
            is AuthState.RefreshingToken -> {
                Log.d(TAG, "Authentication in progress, staying on splash")
                // Stay on splash screen while authenticating
            }
        }
    }
    
    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
    
    private fun navigateToAuth() {
        val intent = Intent(this, AuthActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}