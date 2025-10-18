package com.kevdadev.musicminds.ui.search

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.kevdadev.musicminds.R
import com.kevdadev.musicminds.data.database.entities.Song
import kotlinx.coroutines.launch

class SearchResultsAdapter(
    private val onAddClick: (Song) -> Unit,
    private val isInLibraryCheck: suspend (Song) -> Boolean
) : ListAdapter<Song, SearchResultsAdapter.SearchResultViewHolder>(SongDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchResultViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_song_search_result, parent, false)
        return SearchResultViewHolder(view)
    }

    override fun onBindViewHolder(holder: SearchResultViewHolder, position: Int) {
        val song = getItem(position)
        holder.bind(song)
    }

    inner class SearchResultViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val albumArtImageView: ImageView = itemView.findViewById(R.id.albumArtImageView)
        private val songTitleTextView: TextView = itemView.findViewById(R.id.songTitleTextView)
        private val artistTextView: TextView = itemView.findViewById(R.id.artistTextView)
        private val albumYearTextView: TextView = itemView.findViewById(R.id.albumYearTextView)
        private val addButton: MaterialButton = itemView.findViewById(R.id.addButton)

        fun bind(song: Song) {
            songTitleTextView.text = song.title
            artistTextView.text = song.artist
            albumYearTextView.text = "${song.album} • ${song.releaseYear}"
            
            // TODO: Load album art using an image loading library like Glide or Coil
            // For now, we'll use a placeholder
            albumArtImageView.setImageResource(R.drawable.ic_music_placeholder)
            
            // Check if song is already in library and update button accordingly
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                val isInLibrary = isInLibraryCheck(song)
                updateButtonState(isInLibrary)
            }
            
            addButton.setOnClickListener {
                if (addButton.tag != "added") {
                    onAddClick(song)
                    updateButtonState(true)
                }
            }
        }
        
        private fun updateButtonState(isInLibrary: Boolean) {
            if (isInLibrary) {
                addButton.setIconResource(android.R.drawable.ic_menu_info_details)
                addButton.text = "Added"
                addButton.isEnabled = false
                addButton.tag = "added"
                addButton.alpha = 0.6f
            } else {
                addButton.setIconResource(android.R.drawable.ic_input_add)
                addButton.text = ""
                addButton.isEnabled = true
                addButton.tag = null
                addButton.alpha = 1.0f
            }
        }
    }

    private class SongDiffCallback : DiffUtil.ItemCallback<Song>() {
        override fun areItemsTheSame(oldItem: Song, newItem: Song): Boolean {
            return oldItem.spotifyId == newItem.spotifyId
        }

        override fun areContentsTheSame(oldItem: Song, newItem: Song): Boolean {
            return oldItem == newItem
        }
    }
}