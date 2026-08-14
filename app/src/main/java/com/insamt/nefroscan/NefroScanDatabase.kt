package com.insamt.nefroscan

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [DiagnosticEntity::class, UserEntity::class],
    version = 2,
    exportSchema = false
)
abstract class NefroScanDatabase : RoomDatabase() {

    abstract fun diagnosticDao(): DiagnosticDao
    abstract fun userDao(): UserDao

    companion object {
        @Volatile
        private var INSTANCE: NefroScanDatabase? = null

        fun getDatabase(context: Context): NefroScanDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NefroScanDatabase::class.java,
                    "nefroscan_local_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}