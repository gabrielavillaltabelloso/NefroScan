package com.insamt.nefroscan

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class HistorialActivity : AppCompatActivity() {

    private lateinit var rvHistorial: RecyclerView
    private lateinit var tvHistorialEmpty: TextView
    private val database: NefroScanDatabase by lazy { NefroScanDatabase.getDatabase(applicationContext) }

    private var idUsuario: String = ""
    private var rolUsuario: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_historial)

        // 1. Obtener la sesión activa
        val prefs = getSharedPreferences("SesionNefroScan", Context.MODE_PRIVATE)
        idUsuario = intent.getStringExtra("ID_PACIENTE")
            ?: intent.getStringExtra("ID_MEDICO")
                    ?: intent.getStringExtra("ID_PROMOTOR")
                    ?: prefs.getString("ID_USUARIO", "") ?: ""

        rolUsuario = intent.getStringExtra("EXTRA_ROL")
            ?: prefs.getString("ROL_USUARIO", "PACIENTE") ?: "PACIENTE"

        rvHistorial = findViewById(R.id.rvHistorial)
        tvHistorialEmpty = findViewById(R.id.tvHistorialEmpty)

        rvHistorial.layoutManager = LinearLayoutManager(this)

        // 2. Cargar historial según el rol del usuario conectado
        observarHistorialSegunRol()
    }

    private fun observarHistorialSegunRol() {
        // Seleccionar la consulta reactiva en función del rol
        val flujoDiagnosticos: Flow<List<DiagnosticEntity>> = when (rolUsuario) {
            "MEDICO" -> database.diagnosticDao().obtenerDiagnosticosPorMedico(idUsuario)
            "PROMOTOR" -> database.diagnosticDao().obtenerDiagnosticosPorPromotor(idUsuario)
            else -> database.diagnosticDao().obtenerDiagnosticosPorPaciente(idUsuario)
        }

        lifecycleScope.launch {
            try {
                flujoDiagnosticos.collectLatest { listaExpedientes ->
                    if (listaExpedientes.isEmpty()) {
                        tvHistorialEmpty.text = when (rolUsuario) {
                            "MEDICO" -> "No tienes expedientes clínicos asociados aún."
                            "PROMOTOR" -> "No has registrado tamizajes comunitarios en esta cuenta."
                            else -> "No tienes diagnósticos registrados en tu historial."
                        }
                        tvHistorialEmpty.visibility = View.VISIBLE
                        rvHistorial.visibility = View.GONE
                    } else {
                        tvHistorialEmpty.visibility = View.GONE
                        rvHistorial.visibility = View.VISIBLE
                        rvHistorial.adapter = ExpedienteAdapter(listaExpedientes)
                    }
                }
            } catch (e: Exception) {
                tvHistorialEmpty.text = "Error al cargar los registros locales."
                tvHistorialEmpty.visibility = View.VISIBLE
                rvHistorial.visibility = View.GONE
            }
        }
    }
}