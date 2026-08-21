package com.insamt.nefroscan

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class PromotorDashboardActivity : AppCompatActivity() {

    private var idPromotorSesion: String = ""
    private var nombrePromotorSesion: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_promotor_dashboard)

        // 1. Obtener la sesión activa del promotor de salud
        val prefs = getSharedPreferences("SesionNefroScan", Context.MODE_PRIVATE)
        idPromotorSesion = prefs.getString("ID_USUARIO", "") ?: ""
        nombrePromotorSesion = prefs.getString("NOMBRE_USUARIO", "Promotor de Salud") ?: "Promotor de Salud"

        val btnNuevaVisita = findViewById<Button>(R.id.btnNuevaVisita)
        val btnMapaRiesgo = findViewById<Button>(R.id.btnMapaRiesgo)
        val btnEstadoSincronizacion = findViewById<Button>(R.id.btnEstadoSincronizacion)
        val btnVolverRoles = findViewById<Button>(R.id.btnVolverRoles)

        // 1. Abrir ficha de tamizaje comunitario pasando la trazabilidad del promotor
        btnNuevaVisita.setOnClickListener {
            val intent = Intent(this, PromotorActivity::class.java).apply {
                putExtra("EXTRA_ROL", "PROMOTOR")
                putExtra("EXTRA_REGISTRADOR_ID", idPromotorSesion)
                putExtra("EXTRA_REGISTRADOR_NOMBRE", nombrePromotorSesion)
            }
            startActivity(intent)
        }

        // 2. Abrir radar epidemiológico nacional / departamental
        btnMapaRiesgo.setOnClickListener {
            startActivity(Intent(this, MapaRiesgoActivity::class.java))
        }

        // 3. Abrir gestor de sincronización offline / nube (Firestore)
        btnEstadoSincronizacion.setOnClickListener {
            val intent = Intent(this, SyncStatusActivity::class.java).apply {
                putExtra("ID_PROMOTOR", idPromotorSesion)
            }
            startActivity(intent)
        }

        // 4. Regresar al selector de roles o cerrar sesión
        btnVolverRoles.setOnClickListener {
            finish()
        }
    }
}