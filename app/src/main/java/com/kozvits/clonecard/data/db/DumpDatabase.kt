package com.kozvits.clonecard.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [DumpEntity::class], version = 1, exportSchema = false)
@TypeConverters(DumpConverters::class)
abstract class DumpDatabase : RoomDatabase() {
    abstract fun dumpDao(): DumpDao

    companion object {
        @Volatile
        private var INSTANCE: DumpDatabase? = null

        fun getInstance(context: Context): DumpDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    DumpDatabase::class.java,
                    "clonecard_dumps.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
