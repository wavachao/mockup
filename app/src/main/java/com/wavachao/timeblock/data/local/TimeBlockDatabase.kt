package com.wavachao.timeblock.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [TimeBlockEntity::class],
    version = 2,
    exportSchema = true,
)
abstract class TimeBlockDatabase : RoomDatabase() {

    abstract fun timeBlockDao(): TimeBlockDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE time_blocks ADD COLUMN allDay INTEGER NOT NULL DEFAULT 0")
            }
        }
        private const val NAME = "timeblock.db"

        @Volatile
        private var instance: TimeBlockDatabase? = null

        fun get(context: Context): TimeBlockDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                TimeBlockDatabase::class.java,
                NAME,
            )
                .addMigrations(MIGRATION_1_2)
                .build()
                .also { instance = it }
        }
    }
}
