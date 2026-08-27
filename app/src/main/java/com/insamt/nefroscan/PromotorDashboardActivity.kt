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
        // Enlace de vistas con el layout actualizado
        val btnNuevaVisita = findViewById<Button>(R.id.btnNuevaVisita)
        val btnCalendarioVisitas = findViewById<Button>(R.id.btnCalendarioVisitas)
        val btnHistorialFichas = findViewById<Button>(R.id.btnHistorialFichas)
        val btnDerivacionesMedicas = findViewById<Button>(R.id.btnDerivacionesMedicas)
        val btnChatConsultaRapida = findViewById<Button>(R.id.btnChatConsultaRapida)
        val btnRecursosEducativos = findViewById<Button>(R.id.btnRecursosEducativos)
        val btnMapaRiesgo = findViewById<Button>(R.id.btnMapaRiesgo)
        val btnEstadoSincronizacion = findViewById<Button>(R.id.btnEstadoSincronizacion)
        val btnVolverRoles = findViewById<Button>(R.id.btnVolverRoles)

        // 1. Nueva Visita / Tamizaje y Alertas de Riesgo (CKD)
        btnNuevaVisita.setOnClickListener {
            val intent = Intent(this, PromotorActivity::class.java).apply {
                putExtra("EXTRA_ROL", "PROMOTOR")
                putExtra("EXTRA_REGISTRADOR_ID", idPromotorSesion)
                putExtra("EXTRA_REGISTRADOR_NOMBRE", nombrePromotorSesion)
            }
            startActivity(intent)
        }

        // 2. Rutas y Citas de Control ERC (Calendario de visitas)
        btnCalendarioVisitas.setOnClickListener {
            abrirActivitySegura("AgendaVisitasActivity")
        }

        // 3. Historial de Fichas Comunitarias (Consultas Room locales)
        btnHistorialFichas.setOnClickListener {
            abrirActivitySegura("HistorialFichasActivity")
        }

        // 4. Módulo: Comunicación y Derivaciones Médicas
        btnDerivacionesMedicas.setOnClickListener {
            abrirActivitySegura("DerivacionesMedicasActivity")
        }

        // 5. Chatbot de Consulta Rápida / Chat del Promotor (Preguntas Cerradas de Campo)
        btnChatConsultaRapida.setOnClickListener {
            abrirActivitySegura("ChatPromotor")
        }

        // 6. Educación Comunitaria y Recursos Offline
        btnRecursosEducativos.setOnClickListener {
            abrirActivitySegura("RecursosEducativosActivity")
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

        // 9. Regresar al selector de roles
        btnVolverRoles.setOnClickListener {
            finish()
        }
    }

    /**
     * Permite abrir Activities pasando la trazabilidad de la sesión de forma segura,
     * evitando caídas de la app (Crash) si la clase aún no ha sido creada en el paquete.
     */
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