package com.insamt.nefroscan

import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class DerivacionesMedicasActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_derivaciones_medicas)

        val etPaciente = findViewById<EditText>(R.id.etPacienteDerivacion)
        val etMotivo = findViewById<EditText>(R.id.etMotivoDerivacion)
        val etCentro = findViewById<EditText>(R.id.etCentroSalud)
        val btnCrear = findViewById<Button>(R.id.btnCrearDerivacion)
        val contenedor = findViewById<LinearLayout>(R.id.layoutContenedorDerivaciones)

        agregarTarjeta(contenedor, "Jose Rivera", "Unidad de Salud Cojutepeque", "PA: 160/100 | Proteinuria +++", false)
        agregarTarjeta(contenedor, "Ana Martinez", "Hospital Nacional", "Edema Grado III en miembros inferiores", true)

        btnCrear.setOnClickListener {
            val paciente = etPaciente.text.toString().trim()
            val motivo = etMotivo.text.toString().trim()
            val centro = etCentro.text.toString().trim()

            if (paciente.isEmpty() || motivo.isEmpty() || centro.isEmpty()) {
                Toast.makeText(this, "Completa todos los datos de la derivacion", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            agregarTarjeta(contenedor, paciente, centro, motivo, false, alInicio = true)

            etPaciente.text.clear()
            etMotivo.text.clear()
            etCentro.text.clear()
            Toast.makeText(this, "Derivacion enviada a trazabilidad", Toast.LENGTH_SHORT).show()
        }
    }

    private fun agregarTarjeta(
        contenedor: LinearLayout,
        paciente: String,
        centro: String,
        motivo: String,
        atendidoInicial: Boolean,
        alInicio: Boolean = false
    ) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 20, 24, 20)
            setBackgroundColor(Color.parseColor("#1E293B"))
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 0, 0, 16)
            layoutParams = params
        }

        val tvTitulo = TextView(this).apply {
            text = "Paciente: $paciente"
            setTextColor(Color.WHITE)
            textSize = 15f
            paint.isFakeBoldText = true
        }

        val tvDetalle = TextView(this).apply {
            text = "Destino: $centro\nMotivo: $motivo"
            setTextColor(Color.parseColor("#94A3B8"))
            textSize = 13f
            setPadding(0, 4, 0, 8)
        }

        val tvEstado = TextView(this).apply {
            textSize = 13f
            paint.isFakeBoldText = true
            if (atendidoInicial) {
                text = "● ATENDIDO POR MEDICO"
                setTextColor(Color.parseColor("#10B981"))
            } else {
                text = "● PENDIENTE DE REVISION EN CLINICA"
                setTextColor(Color.parseColor("#F59E0B"))
            }
        }

        val btnActualizar = Button(this).apply {
            text = "Confirmar Recepcion Medica"
            minHeight = 48
            setBackgroundColor(Color.parseColor("#059669"))
            setTextColor(Color.WHITE)
            isEnabled = !atendidoInicial
            if (atendidoInicial) alpha = 0.5f

            setOnClickListener {
                tvEstado.text = "● ATENDIDO POR MEDICO"
                tvEstado.setTextColor(Color.parseColor("#10B981"))
                isEnabled = false
                alpha = 0.5f
                Toast.makeText(this@DerivacionesMedicasActivity, "Trazabilidad actualizada: Atendido", Toast.LENGTH_SHORT).show()
            }
        }

        card.addView(tvTitulo)
        card.addView(tvDetalle)
        card.addView(tvEstado)
        card.addView(btnActualizar)

        if (alInicio) {
            contenedor.addView(card, 0)
        } else {
            contenedor.addView(card)
        }
    }
}