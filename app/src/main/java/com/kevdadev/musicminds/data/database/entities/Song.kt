package com.kevdadev.musicminds.data.database.entities

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import java.util.Date
import java.util.UUID

@Parcelize
@Entity(
    tableName = "songs",
    indices = [Index(value = ["spotify_id"], unique = true)]
)
data class Song(
    @PrimaryKey
    @ColumnInfo(name = "song_id")
    val songId: String = UUID.randomUUID().toString(),
    
    @ColumnInfo(name = "spotify_id")
    val spotifyId: String,
    
    @ColumnInfo(name = "title")
    val title: String,
    
    @ColumnInfo(name = "artist")
    val artist: String,
    
    @ColumnInfo(name = "album")
    val album: String,
    
    @ColumnInfo(name = "release_year")
    val releaseYear: Int,
    
    @ColumnInfo(name = "duration_ms")
    val durationMs: Int,
    
    @ColumnInfo(name = "genre")
    val genre: String? = null,
    
    @ColumnInfo(name = "preview_url")
    val previewUrl: String?,
    
    @ColumnInfo(name = "image_url")
    val imageUrl: String?,
    
    @ColumnInfo(name = "trivia")
    val trivia: String? = null,
    
    @ColumnInfo(name = "date_added")
    val dateAdded: Date = Date()
) : Parcelable