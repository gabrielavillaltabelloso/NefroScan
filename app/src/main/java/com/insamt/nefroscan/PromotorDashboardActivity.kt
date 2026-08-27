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

        configurarVistas()
    }

    private fun configurarVistas() {
        // Enlace de vistas con el layout
        val btnNuevaVisita = findViewById<Button>(R.id.btnNuevaVisita)
        val btnCalendarioVisitas = findViewById<Button>(R.id.btnCalendarioVisitas)
        val btnHistorialFichas = findViewById<Button>(R.id.btnHistorialFichas)
        val btnDerivacionesMedicas = findViewById<Button>(R.id.btnDerivacionesMedicas)
        val btnChatConsultaRapida = findViewById<Button>(R.id.btnChatConsultaRapida)
        val btnRecursosEducativos = findViewById<Button>(R.id.btnRecursosEducativos)
        val btnMapaRiesgo = findViewById<Button>(R.id.btnMapaRiesgo)
        val btnEstadoSincronizacion = findViewById<Button>(R.id.btnEstadoSincronizacion)
        val btnVolverRoles = findViewById<Button>(R.id.btnVolverRoles)

        // 1. Tamizaje y Alertas de Riesgo (CKD)
        btnNuevaVisita.setOnClickListener {
            val intent = Intent(this, PromotorActivity::class.java).apply {
                putExtra("EXTRA_ROL", "PROMOTOR")
                putExtra("EXTRA_REGISTRADOR_ID", idPromotorSesion)
                putExtra("EXTRA_REGISTRADOR_NOMBRE", nombrePromotorSesion)
            }
            startActivity(intent)
        }

        // 2. Rutas y Citas de Control ERC
        btnCalendarioVisitas.setOnClickListener {
            abrirActivitySegura("AgendaVisitasActivity")
        }

        // 3. Historial de Fichas Comunitarias
        btnHistorialFichas.setOnClickListener {
            abrirActivitySegura("HistorialFichasActivity")
        }

        // 4. Casos Derivados y Trazabilidad Médica
        btnDerivacionesMedicas.setOnClickListener {
            abrirActivitySegura("DerivacionesMedicasActivity")
        }

        // 5. Chat de Consulta Rápida
        btnChatConsultaRapida.setOnClickListener {
            abrirActivitySegura("ChatPromotor")
        }

        // 6. Guías y Educación Comunitaria
        btnRecursosEducativos.setOnClickListener {
            abrirActivitySegura("GuiasEducacionActivity")
        }

        // 7. Radar Epidemiológico Nacional
        btnMapaRiesgo.setOnClickListener {
            abrirActivitySegura("MapaRiesgoActivity")
        }

        // 8. Sincronización Offline / Cloud
        btnEstadoSincronizacion.setOnClickListener {
            val intent = Intent(this, SyncStatusActivity::class.java).apply {
                putExtra("ID_PROMOTOR", idPromotorSesion)
                putExtra("NOMBRE_PROMOTOR", nombrePromotorSesion)
            }
            startActivity(intent)
        }

        // 9. Cambiar de Rol
        btnVolverRoles.setOnClickListener {
            finish()
        }
    }

    private fun abrirActivitySegura(nombreClase: String) {
        try {
            val claseDestino = Class.forName("com.insamt.nefroscan.$nombreClase")
            val intent = Intent(this, claseDestino).apply {
                putExtra("EXTRA_ROL", "PROMOTOR")
                putExtra("EXTRA_REGISTRADOR_ID", idPromotorSesion)
                putExtra("EXTRA_REGISTRADOR_NOMBRE", nombrePromotorSesion)
            }
            startActivity(intent)
        } catch (e: ClassNotFoundException) {
            Toast.makeText(this, "Módulo en construcción: $nombreClase", Toast.LENGTH_SHORT).show()
        }
    }
}