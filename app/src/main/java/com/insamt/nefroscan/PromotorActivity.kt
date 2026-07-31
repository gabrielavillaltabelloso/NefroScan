package com.insamt.nefroscan

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class PromotorActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_promotor)

        val etNombre = findViewById<EditText>(R.id.etPromotorNombre)
        val etPA = findViewById<EditText>(R.id.etPromotorPA)
        val spinnerExposicion = findViewById<Spinner>(R.id.spinnerExposicion)
        val btnCalcular = findViewById<Button>(R.id.btnCalcularRiesgoCampo)

        // Llenar el Spinner con opciones de riesgo
        val opciones = arrayOf("Baja exposición (Urbano / Oficina)", "Exposición moderada (Comercio / Campo ocasional)", "Alta exposición constante (Agricultura / Cañales)")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, opciones)
        spinnerExposicion.adapter = adapter

        btnCalcular.setOnClickListener {
            val nombre = etNombre.text.toString().trim()
            val paStr = etPA.text.toString().trim()

            if (nombre.isEmpty() || paStr.isEmpty()) {
                Toast.makeText(this, "Por favor complete todos los campos.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val presionSistolica = paStr.toIntOrNull() ?: 120
            val nivelExposicion = spinnerExposicion.selectedItemPosition // 0, 1 o 2

            // Lógica algorítmica de riesgo comunitario para El Salvador (Nefropatía Mesoamericana)
            var puntajeRiesgo = 0
            if (presionSistolica >= 140) puntajeRiesgo += 2
            else if (presionSistolica >= 130) puntajeRiesgo += 1

            puntajeRiesgo += nivelExposicion

            val (semaforoTexto, colorHex, recomendacion) = when {
                puntajeRiesgo >= 3 -> Triple("ROJO - RIESGO ALTO", "#EF4444", "Derivar urgentemente a Unidad de Salud para perfil renal completo y ecografía.")
                puntajeRiesgo == 2 -> Triple("AMARILLO - RIESGO MODERADO", "#F59E0B", "Programar control de presión e hidratación en menos de 15 días.")
                else -> Triple("VERDE - RIESGO BAJO", "#10B981", "Mantener medidas preventivas de hidratación oral constante.")
            }

            mostrarResultadoCampo(nombre, semaforoTexto, recomendacion)
        }
    }

    private fun mostrarResultadoCampo(nombre: String, semaforo: String, recomendacion: String) {
        AlertDialog.Builder(this)
            .setTitle("Evaluación Comunitaria: $nombre")
            .setMessage("Nivel de Alerta:\n$semaforo\n\nRecomendación Clínica:\n$recomendacion")
            .setPositiveButton("Entendido") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
}