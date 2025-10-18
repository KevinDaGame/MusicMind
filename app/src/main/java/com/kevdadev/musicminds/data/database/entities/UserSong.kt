package com.kevdadev.musicminds.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import java.util.Date

enum class LearningStatus {
    TO_LEARN, LEARNING, LEARNED
}

@Entity(
    tableName = "user_songs",
    primaryKeys = ["user_id", "song_id"],
    foreignKeys = [
        ForeignKey(
            entity = Song::class,
            parentColumns = ["song_id"],
            childColumns = ["song_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["user_id", "learning_status"]),
        Index(value = ["last_attempt_at"]),
        Index(value = ["song_id"])
    ]
)
data class UserSong(
    @ColumnInfo(name = "user_id")
    val userId: String,
    
    @ColumnInfo(name = "song_id")
    val songId: String,
    
    @ColumnInfo(name = "learning_status")
    val learningStatus: LearningStatus = LearningStatus.TO_LEARN,
    
    @ColumnInfo(name = "correct_answers")
    val correctAnswers: Int = 0,
    
    @ColumnInfo(name = "incorrect_answers")
    val incorrectAnswers: Int = 0,
    
    @ColumnInfo(name = "last_attempt_at")
    val lastAttemptAt: Date? = null,
    
    @ColumnInfo(name = "points_earned")
    val pointsEarned: Int = 0,
    
    @ColumnInfo(name = "status_changed_at")
    val statusChangedAt: Date = Date(),
    
    @ColumnInfo(name = "is_favorite")
    val isFavorite: Boolean = false,
    
    @ColumnInfo(name = "title_correct")
    val titleCorrect: Boolean? = null,
    
    @ColumnInfo(name = "artist_correct")
    val artistCorrect: Boolean? = null,
    
    @ColumnInfo(name = "year_correct")
    val yearCorrect: Boolean? = null,
    
    @ColumnInfo(name = "attempt_duration_ms")
    val attemptDurationMs: Int? = null
)