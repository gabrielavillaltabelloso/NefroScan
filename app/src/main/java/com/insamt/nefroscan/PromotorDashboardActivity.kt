package com.insamt.nefroscan

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class PromotorDashboardActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_promotor_dashboard)

        val btnNuevaVisita = findViewById<Button>(R.id.btnNuevaVisita)
        val btnMapaRiesgo = findViewById<Button>(R.id.btnMapaRiesgo)
        val btnEstadoSincronizacion = findViewById<Button>(R.id.btnEstadoSincronizacion)
        val btnVolverRoles = findViewById<Button>(R.id.btnVolverRoles)

        // 1. Abrir ficha de tamizaje comunitario
        btnNuevaVisita.setOnClickListener {
            startActivity(Intent(this, PromotorActivity::class.java))
        }

        // 2. Abrir radar epidemiológico nacional
        btnMapaRiesgo.setOnClickListener {
            startActivity(Intent(this, MapaRiesgoActivity::class.java))
        }

        // 3. Abrir gestor de sincronización offline/nube
        btnEstadoSincronizacion.setOnClickListener {
            startActivity(Intent(this, SyncStatusActivity::class.java))
        }

        // 4. Regresar al selector de roles
        btnVolverRoles.setOnClickListener {
            finish()
        }
    }
}