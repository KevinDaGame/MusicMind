package com.kevdadev.musicminds.ui.library

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kevdadev.musicminds.R
import com.kevdadev.musicminds.data.database.entities.LearningStatus
import kotlinx.coroutines.launch

class LibraryFragment : Fragment() {
    
    companion object {
        private const val ARG_LEARNING_STATUS = "learning_status"
        
        fun newInstance(learningStatus: LearningStatus): LibraryFragment {
            val fragment = LibraryFragment()
            val args = Bundle()
            args.putString(ARG_LEARNING_STATUS, learningStatus.name)
            fragment.arguments = args
            return fragment
        }
    }
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyStateText: TextView
    private lateinit var adapter: LibrarySongAdapter
    private val viewModel: LibraryViewModel by activityViewModels()
    private lateinit var learningStatus: LearningStatus
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        learningStatus = LearningStatus.valueOf(
            arguments?.getString(ARG_LEARNING_STATUS) ?: LearningStatus.TO_LEARN.name
        )
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_library, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        recyclerView = view.findViewById(R.id.recyclerView)
        emptyStateText = view.findViewById(R.id.emptyStateText)
        
        setupRecyclerView()
        observeData()
        
        // Load songs for this category
        viewModel.loadSongsByStatus(learningStatus)
    }
    
    private fun setupRecyclerView() {
        adapter = LibrarySongAdapter { song, newStatus ->
            viewModel.updateSongLearningStatus(song.songId, newStatus)
        }
        
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
    }
    
    private fun observeData() {
        lifecycleScope.launch {
            viewModel.getSongsForStatus(learningStatus).collect { songs ->
                adapter.submitList(songs)
                
                // Show/hide empty state
                if (songs.isEmpty()) {
                    recyclerView.visibility = View.GONE
                    emptyStateText.visibility = View.VISIBLE
                    emptyStateText.text = getEmptyStateMessage()
                } else {
                    recyclerView.visibility = View.VISIBLE
                    emptyStateText.visibility = View.GONE
                }
            }
        }
    }
    
    private fun getEmptyStateMessage(): String {
        return when (learningStatus) {
            LearningStatus.TO_LEARN -> "No songs to learn yet.\nAdd some songs to get started!"
            LearningStatus.LEARNING -> "No songs in progress.\nStart learning some songs!"
            LearningStatus.LEARNED -> "No songs learned yet.\nKeep practicing to see them here!"
        }
    }
}