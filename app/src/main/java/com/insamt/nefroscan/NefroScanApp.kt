package com.insamt.nefroscan

import android.app.Application
import android.util.Log

class NefroScanApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // Capturador global de errores fatales no controlados
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("NEFROSCAN_FATAL", "Crash en hilo ${thread.name}", throwable)
            // Aquí puedes ver el error real antes de que se cierre
            System.exit(2)
        }
    }
}