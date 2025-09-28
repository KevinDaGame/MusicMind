package com.kevdadev.musicminds.ui.splash

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kevdadev.musicminds.auth.AuthRepository
import com.kevdadev.musicminds.auth.data.AuthState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for SplashActivity that manages authentication state checking
 */
class SplashViewModel(application: Application) : AndroidViewModel(application) {
    
    private val authRepository = AuthRepository.getInstance(application)
    
    val authState: StateFlow<AuthState> = authRepository.authState
    
    init {
        // Trigger authentication status check
        viewModelScope.launch {
            authRepository.checkAuthenticationStatus()
        }
    }
}