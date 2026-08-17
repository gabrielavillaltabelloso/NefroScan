package com.insamt.nefroscan

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.insamt.nefroscan.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HistorialActivity : AppCompatActivity() {

    private lateinit var rvHistorial: RecyclerView
    private lateinit var emptyStateLayout: LinearLayout
    private lateinit var tvHistorialEmpty: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_historial)

        rvHistorial = findViewById(R.id.rvHistorial)
        emptyStateLayout = findViewById(R.id.emptyStateLayout)
        tvHistorialEmpty = findViewById(R.id.tvHistorialEmpty)

        rvHistorial.layoutManager = LinearLayoutManager(this)

        cargarHistorialSegunRol()
    }

    private fun cargarHistorialSegunRol() {
        val prefs = getSharedPreferences("SesionNefroScan", Context.MODE_PRIVATE)
        val idActivo = intent.getStringExtra("ID_MEDICO")
            ?: prefs.getString("ID_USUARIO", "") ?: ""
        val rolActivo = intent.getStringExtra("EXTRA_ROL")
            ?: prefs.getString("ROL_USUARIO", "PACIENTE") ?: "PACIENTE"

        lifecycleScope.launch(Dispatchers.IO) {
            val db = NefroScanDatabase.getDatabase(applicationContext)

            val listaExpedientes = when (rolActivo) {
                "PACIENTE" -> db.diagnosticDao().obtenerPorPaciente(idActivo)
                "MEDICO" -> db.diagnosticDao().obtenerListaPorMedico(idActivo)
                "PROMOTOR" -> db.diagnosticDao().obtenerPorRegistrador(idActivo)
                else -> emptyList()
            }

            withContext(Dispatchers.Main) {
                if (listaExpedientes.isEmpty()) {
                    emptyStateLayout.visibility = View.VISIBLE
                    rvHistorial.visibility = View.GONE

                    if (rolActivo == "PACIENTE") {
                        tvHistorialEmpty.text = "No tienes diagnósticos registrados aún."
                    } else {
                        tvHistorialEmpty.text = "No has registrado evaluaciones de pacientes todavía."
                    }
                } else {
                    emptyStateLayout.visibility = View.GONE
                    rvHistorial.visibility = View.VISIBLE
                    rvHistorial.adapter = ExpedienteAdapter(listaExpedientes)
                }
            }
        }
    }
}