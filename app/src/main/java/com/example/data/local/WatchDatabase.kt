package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [WatchVideoEntity::class], version = 1, exportSchema = false)
abstract class WatchDatabase : RoomDatabase() {
    abstract fun watchVideoDao(): WatchVideoDao

    companion object {
        @Volatile
        private var INSTANCE: WatchDatabase? = null

        fun getInstance(context: Context): WatchDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WatchDatabase::class.java,
                    "youtube_watch_manager.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
