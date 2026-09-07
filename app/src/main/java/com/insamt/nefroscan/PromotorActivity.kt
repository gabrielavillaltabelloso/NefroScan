package com.insamt.nefroscan

import android.content.Intent
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
        val etPADiastolica = findViewById<EditText>(R.id.etPromotorPADiastolica) // Opcional si deseas capturarla
        val spinnerExposicion = findViewById<Spinner>(R.id.spinnerExposicion)
        val spinnerSintomasAlerta = findViewById<Spinner>(R.id.spinnerSintomasAlerta) // <-- 1. Declarar el campo con problemas
        val btnCalcular = findViewById<Button>(R.id.btnCalcularRiesgoCampo)

        // Opciones de riesgo de exposición laboral
        val opcionesExposicion = arrayOf(
            "Baja exposición (Urbano / Oficina)",
            "Exposición moderada (Comercio / Campo ocasional)",
            "Alta exposición constante (Agricultura / Cañales)"
        )
        val adapterExposicion = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, opcionesExposicion)
        spinnerExposicion.adapter = adapterExposicion

        // <-- 2. Añadir opciones al Spinner de Signos de Alerta que no funcionaba
        val opcionesSintomas = arrayOf(
            "Ninguno / Sin signos aparentes",
            "Fatiga extrema o mareos frecuentes",
            "Edema (Hinchazón) en párpados o extremidades",
            "Disuria / Alteraciones urinarias evidentes"
        )
        val adapterSintomas = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, opcionesSintomas)
        spinnerSintomasAlerta.adapter = adapterSintomas

        btnCalcular.setOnClickListener {
            val nombre = etNombre.text.toString().trim()
            val paStr = etPA.text.toString().trim()

            if (nombre.isEmpty() || paStr.isEmpty()) {
                Toast.makeText(this, "Por favor complete todos los campos obligatorios.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val presionSistolica = paStr.toIntOrNull() ?: 120
            val nivelExposicion = spinnerExposicion.selectedItemPosition
            val nivelSintomas = spinnerSintomasAlerta.selectedItemPosition // <-- 3. Capturar la selección del usuario

            // Algoritmo de riesgo comunitario ajustado considerando los síntomas de alerta
            var puntajeRiesgo = 0
            if (presionSistolica >= 140) puntajeRiesgo += 2
            else if (presionSistolica >= 130) puntajeRiesgo += 1

            puntajeRiesgo += nivelExposicion

            // Si presenta signos clínicos avanzados (índice mayor a 0 en el spinner), incrementa el riesgo
            if (nivelSintomas > 0) {
                puntajeRiesgo += 1
            }

            val (semaforoTexto, recomendacion, esRiesgoAlto) = when {
                puntajeRiesgo >= 3 -> Triple(
                    "ROJO - RIESGO ALTO",
                    "Derivar urgentemente para evaluación renal completa y ecografía.",
                    true
                )
                puntajeRiesgo == 2 -> Triple(
                    "AMARILLO - RIESGO MODERADO",
                    "Programar control de presión e hidratación en menos de 15 días.",
                    false
                )
                else -> Triple(
                    "VERDE - RIESGO BAJO",
                    "Mantener medidas preventivas de hidratación oral constante.",
                    false
                )
            }

            mostrarResultadoCampo(nombre, semaforoTexto, recomendacion, esRiesgoAlto)
        }
    }

    private fun mostrarResultadoCampo(
        nombre: String,
        semaforo: String,
        recomendacion: String,
        esRiesgoAlto: Boolean
    ) {
        val builder = AlertDialog.Builder(this)
            .setTitle("Evaluación Comunitaria: $nombre")
            .setMessage("Nivel de Alerta:\n$semaforo\n\nRecomendación Clínica:\n$recomendacion")

        if (esRiesgoAlto) {
            builder.setPositiveButton("Realizar Ecografía Inmediata") { dialog, _ ->
                dialog.dismiss()
                val intent = Intent(this, RegistroActivity::class.java).apply {
                    putExtra("EXTRA_NOMBRE", nombre)
                    putExtra("EXTRA_ROL", "PROMOTOR")
                }
                startActivity(intent)
            }
            builder.setNegativeButton("Cerrar") { dialog, _ -> dialog.dismiss() }
        } else {
            builder.setPositiveButton("Entendido") { dialog, _ -> dialog.dismiss() }
        }

        builder.show()
    }
}