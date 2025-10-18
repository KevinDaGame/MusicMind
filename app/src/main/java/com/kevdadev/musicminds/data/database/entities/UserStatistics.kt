package com.kevdadev.musicminds.data.database.entities

data class UserStatistics(
    val totalSongs: Int,
    val learnedSongs: Int,
    val learningSongs: Int,
    val toLearnSongs: Int,
    val totalPoints: Int
)