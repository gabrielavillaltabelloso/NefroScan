package com.insamt.nefroscan

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MedicoDashboardActivity : AppCompatActivity() {

    private lateinit var tvAlerta: TextView
    private lateinit var tvTotal: TextView
    private val database: NefroScanDatabase by lazy { NefroScanDatabase.getDatabase(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_medico_dashboard)

        tvAlerta = findViewById(R.id.tvAlertaPromotor)
        tvTotal = findViewById(R.id.tvTotalExpedientesMed)

        val btnNuevo = findViewById<Button>(R.id.btnNuevoDiagnostico)
        val btnExpedientes = findViewById<Button>(R.id.btnExpedientesClinicos)
        val btnRadar = findViewById<Button>(R.id.btnRadarMed)
        val btnVolver = findViewById<Button>(R.id.btnVolverRolesMed)

        // Cargar métricas e interconexión con las alertas enviadas por el promotor
        cargarResumenMedico()

        // 1. Abrir Registro para nueva ecografía e inferencia CNN / 3D
        btnNuevo.setOnClickListener {
            val intent = Intent(this, RegistroActivity::class.java).apply {
                putExtra("EXTRA_ROL", "MEDICO")
            }
            startActivity(intent)
        }

        // 2. Abrir lista de expedientes clínicos completos
        btnExpedientes.setOnClickListener {
            startActivity(Intent(this, HistorialActivity::class.java))
        }

        // 3. Abrir Mapa de Calor / Radar Epidemiológico
        btnRadar.setOnClickListener {
            startActivity(Intent(this, MapaRiesgoActivity::class.java))
        }

        btnVolver.setOnClickListener { finish() }
    }

    private fun cargarResumenMedico() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val expedientes = database.diagnosticDao().obtenerTodos()
                val casosRiesgoAlto = expedientes.count { it.esRiesgoAlto }
                val total = expedientes.size

                withContext(Dispatchers.Main) {
                    tvTotal.text = "Total Expedientes Evaluados: $total"
                    if (casosRiesgoAlto > 0) {
                        tvAlerta.text = "⚠️ Se han detectado $casosRiesgoAlto pacientes con RIESGO ALTO derivados desde el tamizaje de campo."
                    } else {
                        tvAlerta.text = "✅ No hay derivaciones urgentes pendientes de revisión."
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    tvAlerta.text = "Módulo listo para evaluaciones diagnósticas."
                    tvTotal.text = "Total Expedientes Evaluados: 0"
                }
            }
        }
    }
}