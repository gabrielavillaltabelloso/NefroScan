package com.insamt.nefroscan

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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
import com.google.android.material.chip.ChipGroup
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class CentroAnaliticaMedicoActivity : AppCompatActivity() {

    private lateinit var chartLineProgression: LineChart
    private lateinit var chartBarEstadios: BarChart
    private lateinit var tvInsightIA: TextView
    private lateinit var tvTotalPacientes: TextView
    private lateinit var tvPromedioEgfR: TextView
    private lateinit var tvRiesgoAlto: TextView
    private lateinit var tvBadgeNotifAnalitica: TextView
    private lateinit var btnNotificacionesAnalitica: FrameLayout
    private lateinit var chipGroupTiempo: ChipGroup
    private lateinit var btnExportarInforme: MaterialButton

    private val database: NefroScanDatabase by lazy { NefroScanDatabase.getDatabase(applicationContext) }
    private var idMedicoSesion: String = ""
    private var nombreMedicoSesion: String = ""
    private var listaExpedientesCompleta: List<DiagnosticEntity> = emptyList()
    private var listaExpedientesFiltrada: List<DiagnosticEntity> = emptyList()

    companion object {
        private const val REQUEST_WRITE_PERMISSION = 101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_centro_analitica_medico)

        val prefs = getSharedPreferences("SesionNefroScan", Context.MODE_PRIVATE)
        idMedicoSesion = prefs.getString("ID_USUARIO", "") ?: ""
        nombreMedicoSesion = prefs.getString("NOMBRE_USUARIO", "Especialista") ?: "Especialista"

        // Referencias de vistas
        chartLineProgression = findViewById(R.id.chartLineProgression)
        chartBarEstadios = findViewById(R.id.chartBarEstadios)
        tvInsightIA = findViewById(R.id.tvInsightIA)
        tvTotalPacientes = findViewById(R.id.tvTotalPacientes)
        tvPromedioEgfR = findViewById(R.id.tvPromedioEgfR)
        tvRiesgoAlto = findViewById(R.id.tvRiesgoAlto)
        tvBadgeNotifAnalitica = findViewById(R.id.tvBadgeNotifAnalitica)
        btnNotificacionesAnalitica = findViewById(R.id.btnNotificacionesAnalitica)
        chipGroupTiempo = findViewById(R.id.chipGroupTiempo)
        btnExportarInforme = findViewById(R.id.btnExportarInforme)

        // Configurar toolbar
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbarAnalitica)
        toolbar.setNavigationOnClickListener { finish() }

        // Listener de chips
        chipGroupTiempo.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                filtrarExpedientesPorPeriodo(checkedIds[0])
            }
        }

        // Notificaciones
        btnNotificacionesAnalitica.setOnClickListener {
            val alto = listaExpedientesFiltrada.count { it.egfrEstimado5Anios < 30 }
            AlertDialog.Builder(this)
                .setTitle("Alertas de Riesgo Alto")
                .setMessage("Tiene $alto paciente(s) con eGFR < 30 mL/min (estadios G4/G5) en el período seleccionado.")
                .setPositiveButton("Cerrar", null)
                .show()
        }

        // Exportar PDF
        btnExportarInforme.setOnClickListener {
            if (listaExpedientesFiltrada.isNotEmpty()) {
                exportarInformePdf()
            } else {
                Toast.makeText(this, "No hay datos para exportar.", Toast.LENGTH_SHORT).show()
            }
        }

        // Insight IA clickeable para simulación
        tvInsightIA.setOnClickListener {
            if (listaExpedientesFiltrada.isNotEmpty()) {
                val ultimo = listaExpedientesFiltrada.first()
                mostrarSimuladorKaplanMeier(
                    egfr = ultimo.egfrEstimado5Anios,
                    edad = ultimo.edadPaciente.toDouble(),
                    dano = ultimo.porcentajeDano
                )
            }
        }

        // Cargar datos iniciales (sin filtro)
        cargarDatosIniciales()
    }

    private fun cargarDatosIniciales() {
        lifecycleScope.launch(Dispatchers.IO) {
            val expedientes = database.diagnosticDao().obtenerListaPorMedico(idMedicoSesion)
            listaExpedientesCompleta = expedientes
            listaExpedientesFiltrada = expedientes

            withContext(Dispatchers.Main) {
                actualizarUI()
            }
        }
    }

    private fun filtrarExpedientesPorPeriodo(checkedChipId: Int) {
        val now = Calendar.getInstance()
        val cutoff = now.clone() as Calendar

        when (checkedChipId) {
            R.id.chip3Meses -> cutoff.add(Calendar.MONTH, -3)
            R.id.chip6Meses -> cutoff.add(Calendar.MONTH, -6)
            R.id.chip1Ano -> cutoff.add(Calendar.YEAR, -1)
            R.id.chipTodo -> {
                listaExpedientesFiltrada = listaExpedientesCompleta
                actualizarUI()
                return
            }
            else -> return
        }

        val cutoffMillis = cutoff.timeInMillis
        listaExpedientesFiltrada = listaExpedientesCompleta.filter {
            it.fechaRegistroTimestamp >= cutoffMillis
        }
        actualizarUI()
    }

    private fun actualizarUI() {
        if (listaExpedientesFiltrada.isEmpty()) {
            tvTotalPacientes.text = "0"
            tvPromedioEgfR.text = "0.0"
            tvRiesgoAlto.text = "0"
            tvBadgeNotifAnalitica.text = "0"
            tvInsightIA.text = "No hay registros clínicos suficientes para proyectar tendencias analíticas."
            chartLineProgression.clear()
            chartBarEstadios.clear()
            return
        }

        // KPIs
        val total = listaExpedientesFiltrada.size
        val promedio = listaExpedientesFiltrada.map { it.egfrEstimado5Anios }.average()
        val alto = listaExpedientesFiltrada.count { it.egfrEstimado5Anios < 30 }

        tvTotalPacientes.text = total.toString()
        tvPromedioEgfR.text = String.format(Locale.US, "%.1f", promedio)
        tvRiesgoAlto.text = alto.toString()
        tvBadgeNotifAnalitica.text = alto.toString()

        // Gráficos
        configurarGraficoLineas(listaExpedientesFiltrada)
        configurarGraficoBarras(listaExpedientesFiltrada)

        // Insight
        generarInsightIA(listaExpedientesFiltrada)
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
            valueTextColor = Color.DKGRAY
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
            valueTextColor = Color.DKGRAY
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }

        chartLineProgression.apply {
            data = LineData(dataSetEGFR, dataSetDano)
            description.isEnabled = false
            legend.textColor = Color.DKGRAY
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.textColor = Color.DKGRAY
            xAxis.setDrawGridLines(false)
            axisLeft.textColor = Color.DKGRAY
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
                Color.parseColor("#10B981"),
                Color.parseColor("#38BDF8"),
                Color.parseColor("#F59E0B"),
                Color.parseColor("#F97316"),
                Color.parseColor("#EF4444")
            )
            valueTextColor = Color.DKGRAY
            valueTextSize = 11f
        }

        chartBarEstadios.apply {
            data = BarData(dataSet)
            description.isEnabled = false
            legend.isEnabled = false
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                valueFormatter = IndexAxisValueFormatter(arrayOf("G1", "G2", "G3", "G4", "G5"))
                textColor = Color.DKGRAY
                setDrawGridLines(false)
                granularity = 1f
            }
            axisLeft.textColor = Color.DKGRAY
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
            • Daño Parenquimatoso Medio: ${"%.1f".format(Locale.US, promedioDano)}%.
            • Algoritmo Predictivo: Se sugiere ajustar planes de hidratación y seguimiento quincenal en pacientes con eGFR < 45 mL/min.
            
            👉 Toca este panel para abrir la Simulación de Sobrevida Renal (Modelo Cox / Kaplan-Meier).
        """.trimIndent()
    }

    private fun mostrarSimuladorKaplanMeier(egfr: Double, edad: Double, dano: Double) {
        val sim = CoxSurvivalEngine.simularSobrevida(
            egfr = egfr,
            edad = edad,
            danoPorcentaje = dano,
            usoAines = true,
            hipertensionDescontrolada = true
        )

        val tablaTexto = StringBuilder()
        tablaTexto.append("PROYECCIÓN DE SOBREVIDA RENAL (10 AÑOS)\n")
        tablaTexto.append("Año | Sin Tratamiento | Con NefroScan\n")
        tablaTexto.append("--------------------------------------\n")

        for (p in sim.puntos) {
            val base = String.format(Locale.US, "%.1f%%", p.probabilidadBase)
            val opt = String.format(Locale.US, "%.1f%%", p.probabilidadIntervencion)
            tablaTexto.append("Año ${p.anio.toString().padEnd(2)} | ${base.padEnd(15)} | $opt\n")
        }

        tablaTexto.append("--------------------------------------\n")
        tablaTexto.append("• Ganancia estimada: +${String.format(Locale.US, "%.1f", sim.gananciaAniosLibres)} años libres de diálisis.\n")
        tablaTexto.append("• Reducción del riesgo relativo: ${String.format(Locale.US, "%.1f", sim.reduccionRiesgoRelativo)}%")

        AlertDialog.Builder(this)
            .setTitle("Simulador de Sobrevida (Modelo de Cox)")
            .setMessage(tablaTexto.toString())
            .setPositiveButton("Cerrar", null)
            .show()
    }

    private fun exportarInformePdf() {
        lifecycleScope.launch(Dispatchers.IO) {
            val pdfFile = crearPdfAnalitica(listaExpedientesFiltrada)
            withContext(Dispatchers.Main) {
                if (pdfFile != null) {
                    AlertDialog.Builder(this@CentroAnaliticaMedicoActivity)
                        .setTitle("Informe PDF generado")
                        .setMessage("Se guardó correctamente en:\n${pdfFile.absolutePath}")
                        .setPositiveButton("Abrir PDF") { _, _ -> abrirPdf(pdfFile) }
                        .setNegativeButton("Cerrar", null)
                        .show()
                } else {
                    Toast.makeText(this@CentroAnaliticaMedicoActivity, "Error al generar el PDF.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun crearPdfAnalitica(expedientes: List<DiagnosticEntity>): File? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_WRITE_PERMISSION)
                return null
            }
        }

        val pdfDocument = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint()

        canvas.drawColor(Color.WHITE)

        paint.color = Color.parseColor("#0077B6")
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), 120f, paint)

        paint.color = Color.WHITE
        paint.textSize = 22f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("INSTITUTO NACIONAL DE SAN MIGUEL TEPEZONTES", pageWidth / 2f, 40f, paint)

        paint.textSize = 16f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Reporte Analítico NefroScan AI - Dashboard Clínico", pageWidth / 2f, 70f, paint)

        paint.color = Color.parseColor("#00B4D8")
        canvas.drawRect(0f, 120f, pageWidth.toFloat(), 125f, paint)

        paint.color = Color.parseColor("#03045E")
        paint.textAlign = Paint.Align.LEFT
        paint.textSize = 14f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val leftMargin = 40f
        val rightMargin = pageWidth - 40f
        var y = 160f

        canvas.drawText("Médico: $nombreMedicoSesion (ID: $idMedicoSesion)", leftMargin, y, paint)
        y += 25
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        canvas.drawText("Fecha de generación: ${dateFormat.format(Date())}", leftMargin, y, paint)
        y += 30

        paint.color = Color.parseColor("#E2E8F0")
        canvas.drawLine(leftMargin, y, rightMargin, y, paint)
        y += 30

        paint.color = Color.parseColor("#03045E")
        paint.textSize = 16f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("RESUMEN DE MÉTRICAS", leftMargin, y, paint)
        y += 30

        paint.textSize = 13f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.color = Color.BLACK

        val total = expedientes.size
        val promedio = expedientes.map { it.egfrEstimado5Anios }.average()
        val alto = expedientes.count { it.egfrEstimado5Anios < 30 }

        canvas.drawText("Total de pacientes: $total", leftMargin, y, paint)
        y += 22
        canvas.drawText("eGFR promedio: ${"%.1f".format(Locale.US, promedio)} mL/min/1.73m²", leftMargin, y, paint)
        y += 22
        canvas.drawText("Pacientes en riesgo alto (G4/G5): $alto", leftMargin, y, paint)
        y += 30

        paint.color = Color.parseColor("#03045E")
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("DISTRIBUCIÓN POR ESTADIO KDIGO", leftMargin, y, paint)
        y += 25

        var g1 = 0; var g2 = 0; var g3 = 0; var g4 = 0; var g5 = 0
        expedientes.forEach { exp ->
            when {
                exp.egfrEstimado5Anios >= 90 -> g1++
                exp.egfrEstimado5Anios >= 60 -> g2++
                exp.egfrEstimado5Anios >= 30 -> g3++
                exp.egfrEstimado5Anios >= 15 -> g4++
                else -> g5++
            }
        }

        val filas = listOf(
            "G1 (>=90)" to g1.toString(),
            "G2 (60-89)" to g2.toString(),
            "G3 (30-59)" to g3.toString(),
            "G4 (15-29)" to g4.toString(),
            "G5 (<15)" to g5.toString()
        )

        val rowHeight = 25f
        var rowTop = y
        val col1Width = 200f
        val tableLeft = leftMargin
        val tableRight = rightMargin

        paint.color = Color.parseColor("#E8F4FD")
        canvas.drawRect(tableLeft, rowTop, tableRight, rowTop + rowHeight, paint)
        paint.color = Color.parseColor("#03045E")
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Estadio", tableLeft + 10, rowTop + 17, paint)
        canvas.drawText("Cantidad", tableLeft + col1Width + 10, rowTop + 17, paint)
        rowTop += rowHeight

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.color = Color.BLACK

        for ((estadio, cantidad) in filas) {
            paint.color = Color.WHITE
            canvas.drawRect(tableLeft, rowTop, tableRight, rowTop + rowHeight, paint)
            paint.color = Color.BLACK
            canvas.drawText(estadio, tableLeft + 10, rowTop + 17, paint)
            canvas.drawText(cantidad, tableLeft + col1Width + 10, rowTop + 17, paint)
            paint.color = Color.parseColor("#E2E8F0")
            canvas.drawLine(tableLeft, rowTop + rowHeight, tableRight, rowTop + rowHeight, paint)
            rowTop += rowHeight
        }

        y = rowTop + 30

        paint.color = Color.parseColor("#03045E")
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("PACIENTES RECIENTES (últimos 5)", leftMargin, y, paint)
        y += 25

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 12f

        val pacientesRecientes = expedientes.take(5)
        for (paciente in pacientesRecientes) {
            val texto = "${paciente.nombrePaciente} - eGFR: ${paciente.egfrEstimado5Anios} mL/min - Daño: ${paciente.porcentajeDano}%"
            canvas.drawText(texto, leftMargin, y, paint)
            y += 20
        }

        paint.color = Color.parseColor("#64748B")
        paint.textSize = 10f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("Generado automáticamente por NefroScan AI - ${dateFormat.format(Date())}", pageWidth / 2f, pageHeight - 30f, paint)

        pdfDocument.finishPage(page)

        val fileName = "Reporte_Analitico_${idMedicoSesion}_${System.currentTimeMillis()}.pdf"

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS + "/NefroScan")
                }
                val uri = contentResolver.insert(MediaStore.Files.getContentUri("external"), contentValues)
                val outputStream = contentResolver.openOutputStream(uri!!)
                pdfDocument.writeTo(outputStream)
                outputStream?.close()
                val tempFile = File(cacheDir, fileName)
                val tempOut = FileOutputStream(tempFile)
                pdfDocument.writeTo(tempOut)
                tempOut.close()
                tempFile
            } else {
                val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "NefroScan")
                if (!dir.exists()) dir.mkdirs()
                val file = File(dir, fileName)
                val outputStream = FileOutputStream(file)
                pdfDocument.writeTo(outputStream)
                outputStream.close()
                file
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            pdfDocument.close()
        }
    }

    private fun abrirPdf(file: File) {
        val uri = Uri.fromFile(file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            flags = Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        startActivity(Intent.createChooser(intent, "Abrir PDF"))
    }
}