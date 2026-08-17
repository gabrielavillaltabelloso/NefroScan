package com.insamt.nefroscan

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CentroAnaliticaMedicoActivity : AppCompatActivity() {

    private lateinit var chartLineProgression: LineChart
    private lateinit var chartBarEstadios: BarChart
    private lateinit var tvInsightIA: TextView
    private lateinit var tvSubtituloAnalitica: TextView

    private val database: NefroScanDatabase by lazy { NefroScanDatabase.getDatabase(applicationContext) }
    private var idMedicoSesion: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_centro_analitica_medico)

        val prefs = getSharedPreferences("SesionNefroScan", Context.MODE_PRIVATE)
        idMedicoSesion = prefs.getString("ID_USUARIO", "") ?: ""
        val nombreMedico = prefs.getString("NOMBRE_USUARIO", "Especialista")

        chartLineProgression = findViewById(R.id.chartLineProgression)
        chartBarEstadios = findViewById(R.id.chartBarEstadios)
        tvInsightIA = findViewById(R.id.tvInsightIA)
        tvSubtituloAnalitica = findViewById(R.id.tvSubtituloAnalitica)
        val btnCerrar = findViewById<MaterialButton>(R.id.btnCerrarAnalitica)

        tvSubtituloAnalitica.text = "Médico a cargo: $nombreMedico ($idMedicoSesion)"
        btnCerrar.setOnClickListener { finish() }

        cargarYRenderizarAnalitica()
    }

    private fun cargarYRenderizarAnalitica() {
        lifecycleScope.launch(Dispatchers.IO) {
            val expedientes = database.diagnosticDao().obtenerListaPorMedico(idMedicoSesion)

            withContext(Dispatchers.Main) {
                if (expedientes.isNotEmpty()) {
                    configurarGraficoLineas(expedientes)
                    configurarGraficoBarras(expedientes)
                    generarInsightIA(expedientes)
                } else {
                    tvInsightIA.text = "No hay registros clínicos suficientes para proyectar tendencias analíticas."
                }
            }
        }
    }

    private fun configurarGraficoLineas(expedientes: List<DiagnosticEntity>) {
        val listaOrdenada = expedientes.take(8).reversed()
        val entriesEGFR = ArrayList<Entry>()
        val entriesDano = ArrayList<Entry>()

        listaOrdenada.forEachIndexed { index, exp ->
            entriesEGFR.add(Entry(index.toFloat(), exp.egfrEstimado5Anios.toFloat()))
            entriesDano.add(Entry(index.toFloat(), exp.porcentajeDano.toFloat()))
        }

        val dataSetEGFR = LineDataSet(entriesEGFR, "eGFR Proyectado (mL/min)").apply {
            color = Color.parseColor("#38BDF8")
            setCircleColor(Color.parseColor("#0284C7"))
            lineWidth = 3f
            circleRadius = 5f
            setDrawCircleHole(false)
            valueTextColor = Color.WHITE
            mode = LineDataSet.Mode.CUBIC_BEZIER
            setDrawFilled(true)
            fillColor = Color.parseColor("#0284C7")
            fillAlpha = 50
        }

        val dataSetDano = LineDataSet(entriesDano, "% Daño Tisular").apply {
            color = Color.parseColor("#EF4444")
            setCircleColor(Color.parseColor("#DC2626"))
            lineWidth = 2.5f
            circleRadius = 4f
            valueTextColor = Color.parseColor("#FCA5A5")
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }

        chartLineProgression.apply {
            data = LineData(dataSetEGFR, dataSetDano)
            description.isEnabled = false
            legend.textColor = Color.WHITE
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.textColor = Color.parseColor("#94A3B8")
            xAxis.setDrawGridLines(false)
            axisLeft.textColor = Color.parseColor("#94A3B8")
            axisRight.isEnabled = false
            animateX(1000)
            invalidate()
        }
    }

    private fun configurarGraficoBarras(expedientes: List<DiagnosticEntity>) {
        var g1 = 0; var g2 = 0; var g3 = 0; var g4 = 0; var g5 = 0

        expedientes.forEach { exp ->
            val egfr = exp.egfrEstimado5Anios
            when {
                egfr >= 90 -> g1++
                egfr >= 60 -> g2++
                egfr >= 30 -> g3++
                egfr >= 15 -> g4++
                else -> g5++
            }
        }

        val entries = ArrayList<BarEntry>().apply {
            add(BarEntry(0f, g1.toFloat()))
            add(BarEntry(1f, g2.toFloat()))
            add(BarEntry(2f, g3.toFloat()))
            add(BarEntry(3f, g4.toFloat()))
            add(BarEntry(4f, g5.toFloat()))
        }

        val dataSet = BarDataSet(entries, "Casos por Estadio").apply {
            colors = listOf(
                Color.parseColor("#10B981"), // G1 Verde
                Color.parseColor("#38BDF8"), // G2 Azul cielo
                Color.parseColor("#F59E0B"), // G3 Amarillo
                Color.parseColor("#F97316"), // G4 Naranja
                Color.parseColor("#EF4444")  // G5 Rojo
            )
            valueTextColor = Color.WHITE
            valueTextSize = 11f
        }

        chartBarEstadios.apply {
            data = BarData(dataSet)
            description.isEnabled = false
            legend.isEnabled = false
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                valueFormatter = IndexAxisValueFormatter(arrayOf("G1", "G2", "G3", "G4", "G5"))
                textColor = Color.WHITE
                setDrawGridLines(false)
                granularity = 1f
            }
            axisLeft.textColor = Color.parseColor("#94A3B8")
            axisRight.isEnabled = false
            animateY(1000)
            invalidate()
        }
    }

    private fun generarInsightIA(expedientes: List<DiagnosticEntity>) {
        val total = expedientes.size
        val criticos = expedientes.count { it.egfrEstimado5Anios < 30 }
        val promedioDano = expedientes.map { it.porcentajeDano }.average()

        tvInsightIA.text = """
            • Cohorte Analizada: $total pacientes activos.
            • Alerta KDIGO: $criticos paciente(s) en riesgo de terapia sustitutiva (G4/G5).
            • Daño Parenquimatoso Medio: ${"%.1f".format(promedioDano)}%.
            • Algoritmo Predictivo: Se sugiere ajustar planes de hidratación y seguimiento quincenal en pacientes con eGFR < 45 mL/min.
        """.trimIndent()
    }
}