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

        // Carga segura protegida contra crashes y pantallas en blanco
        cargarHistorialSeguro()
    }

    private fun cargarHistorialSeguro() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Si tu DAO devuelve Flow, obtenemos la lista usando .first() de manera segura
                val listaExpedientes = database.diagnosticDao().obtenerTodosLosDiagnosticos().first()

                withContext(Dispatchers.Main) {
                    if (listaExpedientes.isEmpty()) {
                        tvHistorialEmpty.visibility = View.VISIBLE
                        rvHistorial.visibility = View.GONE
                    } else {
                        tvHistorialEmpty.visibility = View.GONE
                        rvHistorial.visibility = View.VISIBLE
                        // Asignar adaptador de forma segura
                        rvHistorial.adapter = ExpedienteAdapter(listaExpedientes)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    tvHistorialEmpty.text = "Error al cargar los registros locales."
                    tvHistorialEmpty.visibility = View.VISIBLE
                    rvHistorial.visibility = View.GONE
                }
            }
        }
    }
}