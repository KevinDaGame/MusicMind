package com.kevdadev.musicminds.ui.auth

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kevdadev.musicminds.auth.AuthRepository
import com.kevdadev.musicminds.auth.data.AuthState
import com.spotify.sdk.android.auth.AuthorizationRequest
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for AuthActivity that manages Spotify authentication
 */
class AuthViewModel(application: Application) : AndroidViewModel(application) {
    
    private val authRepository = AuthRepository.getInstance(application)
    
    val authState: StateFlow<AuthState> = authRepository.authState
    
    fun createAuthRequest(): AuthorizationRequest {
        return authRepository.createAuthRequest()
    }
    
    fun handleAuthResult(requestCode: Int, resultCode: Int, data: Intent?) {
        viewModelScope.launch {
            authRepository.handleAuthResponse(requestCode, resultCode, data)
        }
    }
}