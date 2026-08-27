package com.insamt.nefroscan

import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class AgendaVisitasActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_agenda_visitas)

        val etNombre = findViewById<EditText>(R.id.etNombreVisita)
        val etLugar = findViewById<EditText>(R.id.etLugarVisita)
        val btnAgendar = findViewById<Button>(R.id.btnAgendar)
        val contenedor = findViewById<LinearLayout>(R.id.layoutContenedorVisitas)

        btnAgendar.setOnClickListener {
            val nombre = etNombre.text.toString().trim()
            val lugar = etLugar.text.toString().trim()

            if (nombre.isEmpty() || lugar.isEmpty()) {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(24, 20, 24, 20)
                setBackgroundColor(Color.parseColor("#334155"))
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                params.setMargins(0, 0, 0, 16)
                layoutParams = params
            }

            val tvInfo = TextView(this).apply {
                text = "Paciente: $nombre\nUbicación: $lugar\nEstado: Pendiente"
                setTextColor(Color.WHITE)
                textSize = 14f
            }

            val btnCompletar = Button(this).apply {
                text = "Marcar como Realizada"
                setBackgroundColor(Color.parseColor("#10B981"))
                setTextColor(Color.WHITE)
                setOnClickListener {
                    tvInfo.text = "Paciente: $nombre\nUbicación: $lugar\nEstado: COMPLETADA ✓"
                    isEnabled = false
                }
            }

            card.addView(tvInfo)
            card.addView(btnCompletar)
            contenedor.addView(card, 0)

            etNombre.text.clear()
            etLugar.text.clear()
            Toast.makeText(this, "Visita programada", Toast.LENGTH_SHORT).show()
        }
    }
}