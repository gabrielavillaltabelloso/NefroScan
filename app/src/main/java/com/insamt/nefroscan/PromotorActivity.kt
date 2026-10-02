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

        // Opciones de riesgo de exposici�n laboral
        val opcionesExposicion = arrayOf(
            "Baja exposici�n (Urbano / Oficina)",
            "Exposici�n moderada (Comercio / Campo ocasional)",
            "Alta exposici�n constante (Agricultura / Ca�ales)"
        )
        val adapterExposicion = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, opcionesExposicion)
        spinnerExposicion.adapter = adapterExposicion

        // <-- 2. A�adir opciones al Spinner de Signos de Alerta que no funcionaba
        val opcionesSintomas = arrayOf(
            "Ninguno / Sin signos aparentes",
            "Fatiga extrema o mareos frecuentes",
            "Edema (Hinchaz�n) en p�rpados o extremidades",
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
            val nivelSintomas = spinnerSintomasAlerta.selectedItemPosition // <-- 3. Capturar la selecci�n del usuario

            // Algoritmo de riesgo comunitario ajustado considerando los s�ntomas de alerta
            var puntajeRiesgo = 0
            if (presionSistolica >= 140) puntajeRiesgo += 2
            else if (presionSistolica >= 130) puntajeRiesgo += 1

            puntajeRiesgo += nivelExposicion

            // Si presenta signos cl�nicos avanzados (�ndice mayor a 0 en el spinner), incrementa el riesgo
            if (nivelSintomas > 0) {
                puntajeRiesgo += 1
            }

            val (semaforoTexto, recomendacion, esRiesgoAlto) = when {
                puntajeRiesgo >= 3 -> Triple(
                    "ROJO - RIESGO ALTO",
                    "Derivar urgentemente para evaluaci�n renal completa y ecograf�a.",
                    true
                )
                puntajeRiesgo == 2 -> Triple(
                    "AMARILLO - RIESGO MODERADO",
                    "Programar control de presi�n e hidrataci�n en menos de 15 d�as.",
                    false
                )
                else -> Triple(
                    "VERDE - RIESGO BAJO",
                    "Mantener medidas preventivas de hidrataci�n oral constante.",
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
            .setTitle("Evaluaci�n Comunitaria: $nombre")
            .setMessage("Nivel de Alerta:\n$semaforo\n\nRecomendaci�n Cl�nica:\n$recomendacion")

        if (esRiesgoAlto) {
            builder.setPositiveButton("Realizar Ecograf�a Inmediata") { dialog, _ ->
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