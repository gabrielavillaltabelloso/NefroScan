package com.insamt.nefroscan

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SyncStatusActivity : AppCompatActivity() {

    private lateinit var tvPendientesCount: TextView
    private lateinit var tvSyncStatus: TextView
    private lateinit var btnSincronizarAhora: Button

    private val database: NefroScanDatabase by lazy { NefroScanDatabase.getDatabase(applicationContext) }
    private val syncManager: FirebaseSyncManager by lazy { FirebaseSyncManager(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sync_status)

        tvPendientesCount = findViewById(R.id.tvPendientesCount)
        tvSyncStatus = findViewById(R.id.tvSyncStatus)
        btnSincronizarAhora = findViewById(R.id.btnSincronizarAhora)
        val btnVolver = findViewById<Button>(R.id.btnVolverDashboard)

        // Cargar el conteo inicial de fichas guardadas localmente
        actualizarConteoPendientes()

        btnSincronizarAhora.setOnClickListener {
            ejecutarSincronizacionManual()
        }

        btnVolver.setOnClickListener {
            finish()
        }
    }

    private fun actualizarConteoPendientes() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val pendientes = database.diagnosticDao().obtenerPendientesDeSincronizar()
                val total = pendientes.size

                withContext(Dispatchers.Main) {
                    tvPendientesCount.text = total.toString()
                    if (total > 0) {
                        tvSyncStatus.text = "Hay fichas locales pendientes de subida a la nube."
                        tvSyncStatus.setTextColor(
                            ContextCompat.getColor(this@SyncStatusActivity, android.R.color.holo_orange_dark)
                        )
                    } else {
                        tvSyncStatus.text = "Todos los expedientes estan sincronizados con la nube."
                        tvSyncStatus.setTextColor(
                            ContextCompat.getColor(this@SyncStatusActivity, android.R.color.holo_green_dark)
                        )
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    tvPendientesCount.text = "0"
                    tvSyncStatus.text = "Error al consultar la base de datos local."
                }
            }
        }
    }

    private fun ejecutarSincronizacionManual() {
        btnSincronizarAhora.isEnabled = false
        tvSyncStatus.text = "Sincronizando con Firebase Cloud..."

        lifecycleScope.launch(Dispatchers.IO) {
            val subidos = syncManager.sincronizarExpedientesPendientes()
            withContext(Dispatchers.Main) {
                btnSincronizarAhora.isEnabled = true
                if (subidos > 0) {
                    Toast.makeText(
                        this@SyncStatusActivity,
                        "¡Exito! Se sincronizaron $subidos expedientes.",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(
                        this@SyncStatusActivity,
                        "No se encontraron registros pendientes o no hay conexion a internet.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                actualizarConteoPendientes()
            }
        }
    }
}