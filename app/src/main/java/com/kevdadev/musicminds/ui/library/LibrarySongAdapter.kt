package com.kevdadev.musicminds.ui.library

import android.view.ContextMenu
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.kevdadev.musicminds.R
import com.kevdadev.musicminds.data.database.entities.LearningStatus
import com.kevdadev.musicminds.data.database.entities.Song

class LibrarySongAdapter(
    private val onStatusChange: (Song, LearningStatus) -> Unit
) : ListAdapter<Song, LibrarySongAdapter.SongViewHolder>(SongDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_library_song, parent, false)
        return SongViewHolder(view, onStatusChange)
    }
    
    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    class SongViewHolder(
        itemView: View,
        private val onStatusChange: (Song, LearningStatus) -> Unit
    ) : RecyclerView.ViewHolder(itemView), View.OnCreateContextMenuListener, MenuItem.OnMenuItemClickListener {
        
        private val albumArt: ImageView = itemView.findViewById(R.id.albumArt)
        private val titleText: TextView = itemView.findViewById(R.id.titleText)
        private val artistText: TextView = itemView.findViewById(R.id.artistText)
        private val albumText: TextView = itemView.findViewById(R.id.albumText)
        private val yearText: TextView = itemView.findViewById(R.id.yearText)
        
        private var currentSong: Song? = null
        
        init {
            itemView.setOnCreateContextMenuListener(this)
        }
        
        fun bind(song: Song) {
            currentSong = song
            
            titleText.text = song.title
            artistText.text = song.artist
            albumText.text = song.album
            yearText.text = song.releaseYear.toString()
            
            // Load album art
            if (!song.imageUrl.isNullOrEmpty()) {
                Glide.with(itemView.context)
                    .load(song.imageUrl)
                    .placeholder(R.drawable.ic_music_note)
                    .error(R.drawable.ic_music_note)
                    .into(albumArt)
            } else {
                albumArt.setImageResource(R.drawable.ic_music_note)
            }
            
            // Set long press listener
            itemView.setOnLongClickListener {
                false // Return false to show context menu
            }
        }
        
        override fun onCreateContextMenu(
            menu: ContextMenu?,
            v: View?,
            menuInfo: ContextMenu.ContextMenuInfo?
        ) {
            menu?.setHeaderTitle("Move to Category")
            
            val toLearnItem = menu?.add(0, 1, 0, "To Learn")
            toLearnItem?.setOnMenuItemClickListener(this)
            
            val learningItem = menu?.add(0, 2, 0, "Learning")
            learningItem?.setOnMenuItemClickListener(this)
            
            val learnedItem = menu?.add(0, 3, 0, "Learned")
            learnedItem?.setOnMenuItemClickListener(this)
        }
        
        override fun onMenuItemClick(item: MenuItem): Boolean {
            val song = currentSong ?: return false
            
            val newStatus = when (item.itemId) {
                1 -> LearningStatus.TO_LEARN
                2 -> LearningStatus.LEARNING
                3 -> LearningStatus.LEARNED
                else -> return false
            }
            
            onStatusChange(song, newStatus)
            return true
        }
    }
    
    private class SongDiffCallback : DiffUtil.ItemCallback<Song>() {
        override fun areItemsTheSame(oldItem: Song, newItem: Song): Boolean {
            return oldItem.songId == newItem.songId
        }
        
        override fun areContentsTheSame(oldItem: Song, newItem: Song): Boolean {
            return oldItem == newItem
        }
    }
}