package com.insamt.nefroscan

import android.content.Context
import androidx.room.Room

object UserDatabaseFactory {

    @Volatile
    private var currentDatabase: NefroScanDatabase? = null
    private var currentUserId: String? = null

    fun getDatabaseForUser(context: Context, idUsuario: String): NefroScanDatabase {
        if (currentDatabase != null && currentUserId == idUsuario) {
            return currentDatabase!!
        }

        synchronized(this) {
            currentDatabase?.close()

            val safeId = idUsuario.replace("@", "_").replace(".", "_").replace("-", "_")
            val dbName = "nefroscan_user_$safeId.db"

            val instance = Room.databaseBuilder(
                context.applicationContext,
                NefroScanDatabase::class.java,
                dbName
            )
                .fallbackToDestructiveMigration()
                .build()

            currentDatabase = instance
            currentUserId = idUsuario
            return instance
        }
    }

    fun cerrarSesion() {
        synchronized(this) {
            currentDatabase?.close()
            currentDatabase = null
            currentUserId = null
        }
    }
}