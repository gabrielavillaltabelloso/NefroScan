package com.insamt.nefroscan

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HistorialActivity : AppCompatActivity() {

    private lateinit var rvHistorial: RecyclerView
    private lateinit var tvHistorialEmpty: TextView
    private val database: NefroScanDatabase by lazy { NefroScanDatabase.getDatabase(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_historial)

        rvHistorial = findViewById(R.id.rvHistorial)
        tvHistorialEmpty = findViewById(R.id.tvHistorialEmpty)

        rvHistorial.layoutManager = LinearLayoutManager(this)

        // 🚀 Carga filtrada por paciente específico
        cargarHistorialSeguro()
    }

    private fun cargarHistorialSeguro() {
        val nombrePaciente = intent.getStringExtra("EXTRA_NOMBRE_PACIENTE")

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Si viene un nombre de paciente, filtra únicamente sus expedientes
                val listaExpedientes: List<DiagnosticEntity> = if (!nombrePaciente.isNullOrEmpty()) {
                    database.diagnosticDao().obtenerDiagnosticosPorPaciente(nombrePaciente)
                } else {
                    // Si no se envió nombre, recupera los diagnósticos usando el Flow
                    database.diagnosticDao().obtenerTodosLosDiagnosticos().first()
                }

                withContext(Dispatchers.Main) {
                    if (listaExpedientes.isEmpty()) {
                        tvHistorialEmpty.text = if (!nombrePaciente.isNullOrEmpty()) {
                            "No se encontraron expedientes para $nombrePaciente."
                        } else {
                            "No hay registros guardados."
                        }
                        tvHistorialEmpty.visibility = View.VISIBLE
                        rvHistorial.visibility = View.GONE
                    } else {
                        tvHistorialEmpty.visibility = View.GONE
                        rvHistorial.visibility = View.VISIBLE
                        // Asignar adaptador con la lista filtrada del paciente
                        rvHistorial.adapter = ExpedienteAdapter(listaExpedientes)
                    }
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    tvHistorialEmpty.text = "Error al cargar los registros locales."
                    tvHistorialEmpty.visibility = View.VISIBLE
                    rvHistorial.visibility = View.GONE
                }
            }
        }
    }
}