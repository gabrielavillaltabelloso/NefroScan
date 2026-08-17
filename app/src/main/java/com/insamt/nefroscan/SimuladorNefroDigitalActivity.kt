package com.insamt.nefroscan

import android.graphics.Color
import android.os.Bundle
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class SimuladorNefroDigitalActivity : AppCompatActivity() {

    private lateinit var tvSimEgfr5: TextView
    private lateinit var tvSimEgfr10: TextView
    private lateinit var tvDiagnosticoSimulacion: TextView
    private lateinit var lblSimAgua: TextView
    private lateinit var lblSimSodio: TextView
    private lateinit var lblSimPresion: TextView

    private lateinit var sbSimAgua: SeekBar
    private lateinit var sbSimSodio: SeekBar
    private lateinit var sbSimPresion: SeekBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_simulador_nefro_digital)

        tvSimEgfr5 = findViewById(R.id.tvSimEgfr5)
        tvSimEgfr10 = findViewById(R.id.tvSimEgfr10)
        tvDiagnosticoSimulacion = findViewById(R.id.tvDiagnosticoSimulacion)
        lblSimAgua = findViewById(R.id.lblSimAgua)
        lblSimSodio = findViewById(R.id.lblSimSodio)
        lblSimPresion = findViewById(R.id.lblSimPresion)

        sbSimAgua = findViewById(R.id.sbSimAgua)
        sbSimSodio = findViewById(R.id.sbSimSodio)
        sbSimPresion = findViewById(R.id.sbSimPresion)

        findViewById<MaterialButton>(R.id.btnCerrarSimulador).setOnClickListener { finish() }

        val listener = object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                recalcularSimulacion()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        }

        sbSimAgua.setOnSeekBarChangeListener(listener)
        sbSimSodio.setOnSeekBarChangeListener(listener)
        sbSimPresion.setOnSeekBarChangeListener(listener)

        findViewById<MaterialButton>(R.id.btnExportarProtocolo).setOnClickListener {
            Toast.makeText(this, "Protocolo simulado anexado a las recomendaciones clínicas.", Toast.LENGTH_SHORT).show()
            finish()
        }

        recalcularSimulacion()
    }

    private fun recalcularSimulacion() {
        val aguaLitros = sbSimAgua.progress / 10.0f
        val sodioNivel = sbSimSodio.progress
        val sistolica = 90 + sbSimPresion.progress

        lblSimAgua.text = "Meta de Hidratación: ${"%.1f".format(aguaLitros)} L/día"
        lblSimSodio.text = "Consumo de Sodio: " + when (sodioNivel) {
            0 -> "Bajo (<1.5g/día)"
            1 -> "Normal (1.5g - 2.3g/día)"
            else -> "Alto / Hipernatriemia (>2.3g/día)"
        }
        lblSimPresion.text = "Presión Arterial Sistólica: $sistolica mmHg"

        var egfrBase = 95
        if (aguaLitros < 1.5f) egfrBase -= 20
        if (aguaLitros >= 2.5f) egfrBase += 5
        if (sodioNivel == 2) egfrBase -= 15
        if (sistolica > 140) egfrBase -= ((sistolica - 140) * 0.4).toInt()

        egfrBase = egfrBase.coerceIn(15, 120)

        val egfr5 = (egfrBase * 0.90).toInt().coerceIn(10, 130)
        val egfr10 = (egfrBase * 0.75).toInt().coerceIn(10, 120)

        tvSimEgfr5.text = "$egfr5 mL/min"
        tvSimEgfr10.text = "$egfr10 mL/min"

        when {
            egfr10 >= 60 -> {
                tvDiagnosticoSimulacion.text = "Estado: Reserva Estable • Baja Tasa de Deterioro"
                tvDiagnosticoSimulacion.setTextColor(Color.parseColor("#10B981"))
            }
            egfr10 >= 30 -> {
                tvDiagnosticoSimulacion.text = "Estado: Deterioro Moderado • Requiere Ajuste Nefroprotector"
                tvDiagnosticoSimulacion.setTextColor(Color.parseColor("#F59E0B"))
            }
            else -> {
                tvDiagnosticoSimulacion.text = "Estado: Riesgo Alto • Progresión Acelerada a Falla Renal"
                tvDiagnosticoSimulacion.setTextColor(Color.parseColor("#EF4444"))
            }
        }
    }
}