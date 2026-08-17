package com.insamt.nefroscan

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [UserEntity::class, DiagnosticEntity::class],
    version = 2,
    exportSchema = false
)
abstract class NefroScanDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun diagnosticDao(): DiagnosticDao

    companion object {
        @Volatile
        private var INSTANCE: NefroScanDatabase? = null

        fun getDatabase(context: Context): NefroScanDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NefroScanDatabase::class.java,
                    "nefroscan_general_database.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}