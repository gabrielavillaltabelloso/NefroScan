package com.insamt.nefroscan

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

class RecordatorioReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val medicamento = intent.getStringExtra("medicamento") ?: "Medicamento"
        val dosis = intent.getStringExtra("dosis") ?: ""
        val id = intent.getIntExtra("id", 0)

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "nefroscan_recordatorios"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Recordatorios de Medicamentos",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones para recordar tomar medicamentos"
                enableVibration(true)
            }
            manager.createNotificationChannel(channel)
        }

        val openAppIntent = Intent(context, PacienteDashboardActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            id,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val texto = if (dosis.isNotEmpty()) "$medicamento - $dosis" else medicamento

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("Hora de su medicamento")
            .setContentText(texto)
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "Es hora de tomar: $texto\n\nRecuerde seguir las indicaciones de su nefrologo."
                )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        manager.notify(id, notification)
    }
}