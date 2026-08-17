package com.insamt.nefroscan

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TriajeRadarPredictivoActivity : AppCompatActivity() {

    private lateinit var rvTriaje: RecyclerView
    private lateinit var tvResumenTriaje: TextView
    private lateinit var tvTriajeVacio: TextView

    private val database: NefroScanDatabase by lazy { NefroScanDatabase.getDatabase(applicationContext) }
    private var idMedicoSesion: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_triaje_radar_predictivo)

        val prefs = getSharedPreferences("SesionNefroScan", Context.MODE_PRIVATE)
        idMedicoSesion = prefs.getString("ID_USUARIO", "") ?: ""

        rvTriaje = findViewById(R.id.rvTriaje)
        tvResumenTriaje = findViewById(R.id.tvResumenTriaje)
        tvTriajeVacio = findViewById(R.id.tvTriajeVacio)
        findViewById<MaterialButton>(R.id.btnCerrarTriaje).setOnClickListener { finish() }

        rvTriaje.layoutManager = LinearLayoutManager(this)

        cargarTriajePriorizado()
    }

    private fun cargarTriajePriorizado() {
        lifecycleScope.launch(Dispatchers.IO) {
            val expedientes = database.diagnosticDao().obtenerListaPorMedico(idMedicoSesion)
            val ordenadosPorRiesgo = expedientes.sortedByDescending { it.porcentajeDano }

            withContext(Dispatchers.Main) {
                if (ordenadosPorRiesgo.isEmpty()) {
                    tvTriajeVacio.visibility = View.VISIBLE
                    rvTriaje.visibility = View.GONE
                    tvResumenTriaje.text = "0 pacientes evaluados"
                } else {
                    tvTriajeVacio.visibility = View.GONE
                    rvTriaje.visibility = View.VISIBLE
                    tvResumenTriaje.text = "${ordenadosPorRiesgo.size} pacientes ordenados por severidad tisular"
                    rvTriaje.adapter = ExpedienteAdapter(ordenadosPorRiesgo)
                }
            }
        }
    }
}