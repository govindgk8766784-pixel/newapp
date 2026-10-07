package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.StudioProject

@Database(entities = [StudioProject::class], version = 1, exportSchema = false)
abstract class PixelProDatabase : RoomDatabase() {
    abstract fun studioProjectDao(): StudioProjectDao

    companion object {
        @Volatile
        private var INSTANCE: PixelProDatabase? = null

        fun getDatabase(context: Context): PixelProDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PixelProDatabase::class.java,
                    "pixelpro_studio.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
