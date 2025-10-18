package com.kevdadev.musicminds.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.kevdadev.musicminds.data.database.dao.SongDao
import com.kevdadev.musicminds.data.database.entities.Song
import com.kevdadev.musicminds.data.database.entities.UserSong

@Database(
    entities = [Song::class, UserSong::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun songDao(): SongDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "musicminds_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}