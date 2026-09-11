package com.wavachao.timeblock.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [TimeBlockEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class TimeBlockDatabase : RoomDatabase() {

    abstract fun timeBlockDao(): TimeBlockDao

    companion object {
        private const val NAME = "timeblock.db"

        @Volatile
        private var instance: TimeBlockDatabase? = null

        fun get(context: Context): TimeBlockDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                TimeBlockDatabase::class.java,
                NAME,
            )
                .fallbackToDestructiveMigration()
                .build()
                .also { instance = it }
        }
    }
}
