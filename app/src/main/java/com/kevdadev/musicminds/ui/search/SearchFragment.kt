package com.kevdadev.musicminds.ui.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.textfield.TextInputEditText
import com.kevdadev.musicminds.R
import com.kevdadev.musicminds.auth.TokenManager
import com.kevdadev.musicminds.data.api.SpotifyApiService
import com.kevdadev.musicminds.data.database.AppDatabase
import com.kevdadev.musicminds.data.repository.SongRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SearchFragment : Fragment() {
    
    private lateinit var searchViewModel: SearchViewModel
    private lateinit var searchResultsAdapter: SearchResultsAdapter
    
    // UI components
    private lateinit var searchEditText: TextInputEditText
    private lateinit var progressBar: View
    private lateinit var emptyStateLayout: View
    private lateinit var errorStateLayout: View
    private lateinit var searchResultsRecyclerView: androidx.recyclerview.widget.RecyclerView
    private lateinit var errorText: android.widget.TextView
    private lateinit var retryButton: android.widget.Button
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_search, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initViews(view)
        initViewModel()
        setupRecyclerView()
        setupSearchInput()
        observeViewModel()
    }
    
    private fun initViews(view: View) {
        searchEditText = view.findViewById(R.id.searchEditText)
        progressBar = view.findViewById(R.id.progressBar)
        emptyStateLayout = view.findViewById(R.id.emptyStateLayout)
        errorStateLayout = view.findViewById(R.id.errorStateLayout)
        searchResultsRecyclerView = view.findViewById(R.id.searchResultsRecyclerView)
        errorText = view.findViewById(R.id.errorText)
        retryButton = view.findViewById(R.id.retryButton)
    }
    
    private fun initViewModel() {
        val database = AppDatabase.getDatabase(requireContext())
        val tokenManager = TokenManager(requireContext())
        val spotifyApiService = SpotifyApiService(tokenManager)
        val songRepository = SongRepository(database.songDao(), spotifyApiService)
        
        // TODO: Get actual user ID from authentication system
        val userId = "default_user" // Placeholder for now
        
        val factory = SearchViewModelFactory(songRepository, userId)
        searchViewModel = ViewModelProvider(this, factory)[SearchViewModel::class.java]
    }
    
    private fun setupRecyclerView() {
        searchResultsAdapter = SearchResultsAdapter { song ->
            searchViewModel.addSongToLibrary(song)
        }
        
        searchResultsRecyclerView.apply {
            adapter = searchResultsAdapter
            layoutManager = LinearLayoutManager(context)
            addItemDecoration(
                DividerItemDecoration(context, LinearLayoutManager.VERTICAL)
            )
        }
    }
    
    private fun setupSearchInput() {
        searchEditText.doOnTextChanged { text, _, _, _ ->
            searchViewModel.search(text.toString())
        }
        
        searchEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                searchViewModel.search(searchEditText.text.toString())
                true
            } else {
                false
            }
        }
        
        retryButton.setOnClickListener {
            searchViewModel.retryLastSearch()
        }
    }
    
    private fun observeViewModel() {
        // Observe UI state
        lifecycleScope.launch {
            searchViewModel.uiState.collect { uiState ->
                updateUIState(uiState)
            }
        }
        
        // Observe search results
        searchViewModel.searchResults.observe(viewLifecycleOwner) { results ->
            searchResultsAdapter.submitList(results)
        }
        
        // Observe messages
        searchViewModel.message.observe(viewLifecycleOwner) { message ->
            if (message.isNotEmpty()) {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                searchViewModel.clearMessage()
            }
        }
    }
    
    private fun updateUIState(uiState: SearchUiState) {
        // Show/hide progress bar
        progressBar.visibility = if (uiState.isLoading) View.VISIBLE else View.GONE
        
        // Show/hide empty state
        emptyStateLayout.visibility = if (uiState.showEmptyState && !uiState.isLoading) {
            View.VISIBLE
        } else {
            View.GONE
        }
        
        // Show/hide error state
        errorStateLayout.visibility = if (uiState.errorMessage != null && !uiState.isLoading) {
            errorText.text = uiState.errorMessage
            View.VISIBLE
        } else {
            View.GONE
        }
        
        // Show/hide search results
        searchResultsRecyclerView.visibility = if (uiState.searchResults.isNotEmpty() && !uiState.isLoading) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }
    
    companion object {
        fun newInstance(): SearchFragment {
            return SearchFragment()
        }
    }
}