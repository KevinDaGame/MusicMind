package com.kevdadev.musicminds.ui.search

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kevdadev.musicminds.data.database.entities.Song
import com.kevdadev.musicminds.data.repository.SongRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class SearchViewModel(
    private val songRepository: SongRepository,
    private val userId: String
) : ViewModel() {
    
    companion object {
        private const val TAG = "SearchViewModel"
        private const val SEARCH_DELAY_MS = 500L
        private const val MIN_QUERY_LENGTH = 3
    }
    
    // UI State
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()
    
    // Search results
    private val _searchResults = MutableLiveData<List<Song>>()
    val searchResults: LiveData<List<Song>> = _searchResults
    
    // Messages for user feedback
    private val _message = MutableLiveData<String>()
    val message: LiveData<String> = _message
    
    // Search query flow for debouncing
    private val searchQueryFlow = MutableStateFlow("")
    
    init {
        setupSearchDebouncing()
    }
    
    @OptIn(FlowPreview::class)
    private fun setupSearchDebouncing() {
        searchQueryFlow
            .debounce(SEARCH_DELAY_MS)
            .filter { it.isNotBlank() && it.length >= MIN_QUERY_LENGTH }
            .distinctUntilChanged()
            .onEach { query ->
                performSearch(query)
            }
            .launchIn(viewModelScope)
    }
    
    fun search(query: String) {
        Log.d(TAG, "Search query: $query")
        
        if (query.isBlank()) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                searchResults = emptyList(),
                showEmptyState = true,
                errorMessage = null
            )
            _searchResults.value = emptyList()
            return
        }
        
        if (query.length < MIN_QUERY_LENGTH) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                searchResults = emptyList(),
                showEmptyState = true,
                errorMessage = null
            )
            return
        }
        
        // Update UI to show loading
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            showEmptyState = false,
            errorMessage = null
        )
        
        // Update search query flow
        searchQueryFlow.value = query
    }
    
    private fun performSearch(query: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Performing search for: $query")
                
                val result = songRepository.searchSpotifyTracks(query)
                
                result.fold(
                    onSuccess = { songs ->
                        Log.d(TAG, "Search successful, found ${songs.size} songs")
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            searchResults = songs,
                            showEmptyState = songs.isEmpty(),
                            errorMessage = null
                        )
                        _searchResults.value = songs
                    },
                    onFailure = { exception ->
                        Log.e(TAG, "Search failed", exception)
                        val errorMessage = when {
                            exception.message?.contains("token") == true -> 
                                "Authentication expired. Please log in again."
                            exception.message?.contains("network") == true || 
                            exception.message?.contains("timeout") == true ->
                                "Network error. Please check your connection."
                            else -> "Search failed. Please try again."
                        }
                        
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            searchResults = emptyList(),
                            showEmptyState = false,
                            errorMessage = errorMessage
                        )
                        _searchResults.value = emptyList()
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Exception in performSearch", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    searchResults = emptyList(),
                    showEmptyState = false,
                    errorMessage = "An unexpected error occurred"
                )
                _searchResults.value = emptyList()
            }
        }
    }
    
    fun addSongToLibrary(song: Song) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Adding song to library: ${song.title}")
                
                val result = songRepository.addSongToUserLibrary(song, userId)
                
                result.fold(
                    onSuccess = { wasAdded ->
                        if (wasAdded) {
                            _message.value = "\"${song.title}\" added to your library!"
                            Log.d(TAG, "Song added successfully: ${song.title}")
                        } else {
                            _message.value = "\"${song.title}\" is already in your library"
                            Log.d(TAG, "Song already exists: ${song.title}")
                        }
                    },
                    onFailure = { exception ->
                        Log.e(TAG, "Failed to add song to library", exception)
                        _message.value = "Failed to add song. Please try again."
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Exception in addSongToLibrary", e)
                _message.value = "Failed to add song. Please try again."
            }
        }
    }
    
    fun retryLastSearch() {
        val currentQuery = searchQueryFlow.value
        if (currentQuery.isNotBlank()) {
            performSearch(currentQuery)
        }
    }
    
    fun clearMessage() {
        _message.value = ""
    }
}

data class SearchUiState(
    val isLoading: Boolean = false,
    val searchResults: List<Song> = emptyList(),
    val showEmptyState: Boolean = true,
    val errorMessage: String? = null
)

class SearchViewModelFactory(
    private val songRepository: SongRepository,
    private val userId: String
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SearchViewModel::class.java)) {
            return SearchViewModel(songRepository, userId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}