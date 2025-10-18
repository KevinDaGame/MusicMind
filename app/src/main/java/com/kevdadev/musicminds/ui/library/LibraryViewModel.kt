package com.kevdadev.musicminds.ui.library

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kevdadev.musicminds.auth.AuthRepository
import com.kevdadev.musicminds.auth.TokenManager
import com.kevdadev.musicminds.data.api.SpotifyApiService
import com.kevdadev.musicminds.data.database.AppDatabase
import com.kevdadev.musicminds.data.database.dao.SongDao
import com.kevdadev.musicminds.data.database.entities.LearningStatus
import com.kevdadev.musicminds.data.database.entities.Song
import com.kevdadev.musicminds.data.repository.SongRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch

class LibraryViewModel(application: Application) : AndroidViewModel(application) {
    
    companion object {
        private const val TAG = "LibraryViewModel"
    }
    
    private val authRepository = AuthRepository.getInstance(application)
    private val database = AppDatabase.getDatabase(application)
    private val songRepository = SongRepository(database.songDao(), SpotifyApiService(TokenManager(application)))
    
    private val _categoryCounts = MutableStateFlow(SongDao.CategoryCounts(0, 0, 0))
    val categoryCounts: StateFlow<SongDao.CategoryCounts> = _categoryCounts.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    init {
        loadCategoryCounts()
    }
    
    fun getSongsForStatus(status: LearningStatus): Flow<List<Song>> {
        val currentUser = authRepository.getCurrentUser()
        return if (currentUser != null) {
            songRepository.getUserSongsByStatus(currentUser.id, status)
        } else {
            Log.w(TAG, "No authenticated user found")
            emptyFlow()
        }
    }
    
    fun loadSongsByStatus(status: LearningStatus) {
        // This method can be used to trigger loading if needed
        // The Flow in getSongsForStatus will automatically update
        Log.d(TAG, "Loading songs for status: $status")
    }
    
    fun updateSongLearningStatus(songId: String, newStatus: LearningStatus) {
        val currentUser = authRepository.getCurrentUser()
        if (currentUser == null) {
            Log.w(TAG, "Cannot update song status: no authenticated user")
            return
        }
        
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = songRepository.updateSongLearningStatus(currentUser.id, songId, newStatus)
                result.fold(
                    onSuccess = {
                        Log.d(TAG, "Successfully updated song learning status")
                        loadCategoryCounts() // Refresh counts
                    },
                    onFailure = { exception ->
                        Log.e(TAG, "Failed to update song learning status", exception)
                    }
                )
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    private fun loadCategoryCounts() {
        val currentUser = authRepository.getCurrentUser()
        if (currentUser == null) {
            Log.w(TAG, "Cannot load category counts: no authenticated user")
            return
        }
        
        viewModelScope.launch {
            try {
                val counts = songRepository.getCategoryCounts(currentUser.id)
                _categoryCounts.value = counts
                Log.d(TAG, "Category counts loaded: $counts")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load category counts", e)
            }
        }
    }
    
    fun refreshCategoryCounts() {
        loadCategoryCounts()
    }
}