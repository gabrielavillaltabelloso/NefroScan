package com.insamt.nefroscan

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.insamt.nefroscan.data.model.EdemaEvaluacion

@Database(
    entities = [UserEntity::class, DiagnosticEntity::class, EdemaEvaluacion::class],
    version = 3,
    exportSchema = false
)
abstract class NefroScanDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun diagnosticDao(): DiagnosticDao
    abstract fun edemaDao(): EdemaDao // <-- Declaración del DAO de Edema

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