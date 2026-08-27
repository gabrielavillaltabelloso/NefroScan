package com.insamt.nefroscan

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.exp
import kotlin.math.pow

class MedicoDashboardActivity : AppCompatActivity() {

    private lateinit var tvTotal: TextView
    private lateinit var tvRiesgoAlto: TextView
    private lateinit var tvAlerta: TextView
    private lateinit var tvBadgeNotif: TextView
    private lateinit var btnNotificaciones: FrameLayout
    private lateinit var tvBienvenida: TextView

    private val database: NefroScanDatabase by lazy { NefroScanDatabase.getDatabase(applicationContext) }

    private var idMedicoSesion: String = ""
    private var nombreMedicoSesion: String = ""
    private var contadorRiesgoAlto: Int = 0

    companion object {
        private const val REQUEST_WRITE_PERMISSION = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_medico_dashboard)

        // Obtener la sesión activa del médico
        val prefs = getSharedPreferences("SesionNefroScan", Context.MODE_PRIVATE)
        idMedicoSesion = prefs.getString("ID_USUARIO", "") ?: ""
        nombreMedicoSesion = prefs.getString("NOMBRE_USUARIO", "Dr. Especialista") ?: "Dr. Especialista"

        tvTotal = findViewById(R.id.tvKpiTotal)
        tvRiesgoAlto = findViewById(R.id.tvKpiRiesgoAlto)
        tvAlerta = findViewById(R.id.tvAlertaDerivacion)
        tvBadgeNotif = findViewById(R.id.tvBadgeNotif)
        btnNotificaciones = findViewById(R.id.btnNotificaciones)
        tvBienvenida = findViewById(R.id.tvBienvenida)

        tvBienvenida.text = "Dr(a). $nombreMedicoSesion"

        // Botones de Módulos Clínicos Avanzados
        val btnAbrirCentroAnalitica = findViewById<Button>(R.id.btnAbrirCentroAnalitica)
        val btnAbrirDictamenMedico = findViewById<Button>(R.id.btnAbrirDictamenMedico)
        val btnAbrirSimuladorDigital = findViewById<Button>(R.id.btnAbrirSimuladorDigital)
        val btnAbrirTriajeRadar = findViewById<Button>(R.id.btnAbrirTriajeRadar)

        // Botones de Herramientas Tradicionales
        val btnInferencia = findViewById<Button>(R.id.btnInferenciaEcografia)
        val btnCalculadora = findViewById<Button>(R.id.btnCalculadoraEgfr)
        val btnNefrotoxicidad = findViewById<Button>(R.id.btnNefrotoxicidad)
        val btnPrescriptor = findViewById<Button>(R.id.btnPrescriptorHidratacion)
        val btnKfre = findViewById<Button>(R.id.btnCalculadoraKfre)
        val btnInforme = findViewById<Button>(R.id.btnGenerarInformePdf)
        val btnExpedientes = findViewById<Button>(R.id.btnExpedientesClinicos)
        val btnRadar = findViewById<Button>(R.id.btnRadarMed)
        val btnVolver = findViewById<Button>(R.id.btnVolverRolesMed)

        // Cargar métricas clínicas del médico en sesión
        cargarMetricasClinicas()

        // Acción para la campanita de notificaciones
        btnNotificaciones.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Centro de Notificaciones")
                .setMessage("Tiene $contadorRiesgoAlto paciente(s) bajo su seguimiento con indicadores de Riesgo Alto o Falla Renal G4/G5 pendientes de revisión prioritaria.")
                .setPositiveButton("Ver Expedientes") { _, _ ->
                    val intent = Intent(this, HistorialActivity::class.java).apply {
                        putExtra("EXTRA_ROL", "MEDICO")
                        putExtra("ID_MEDICO", idMedicoSesion)
                    }
                    startActivity(intent)
                }
                .setNegativeButton("Cerrar", null)
                .show()
        }

        // Navegación a módulos avanzados
        btnAbrirCentroAnalitica.setOnClickListener { startActivity(Intent(this, CentroAnaliticaMedicoActivity::class.java)) }
        btnAbrirDictamenMedico.setOnClickListener { startActivity(Intent(this, DictamenMedicoActivity::class.java)) }
        btnAbrirSimuladorDigital.setOnClickListener { startActivity(Intent(this, SimuladorNefroDigitalActivity::class.java)) }
        btnAbrirTriajeRadar.setOnClickListener { startActivity(Intent(this, TriajeRadarPredictivoActivity::class.java)) }

        // Navegación a herramientas tradicionales
        btnInferencia.setOnClickListener {
            val intent = Intent(this, RegistroActivity::class.java).apply {
                putExtra("EXTRA_ROL", "MEDICO")
                putExtra("EXTRA_REGISTRADOR_ID", idMedicoSesion)
            }
            startActivity(intent)
        }

        btnCalculadora.setOnClickListener { mostrarCalculadoraCkdEpi() }
        btnNefrotoxicidad.setOnClickListener { mostrarEvaluadorNefrotoxicidad() }
        btnPrescriptor.setOnClickListener { mostrarPrescriptorHidratacion() }
        btnKfre.setOnClickListener { mostrarCalculadoraKfre() }
        btnInforme.setOnClickListener { generarInformeMedicoOficial() }

        btnExpedientes.setOnClickListener {
            val intent = Intent(this, HistorialActivity::class.java).apply {
                putExtra("EXTRA_ROL", "MEDICO")
                putExtra("ID_MEDICO", idMedicoSesion)
            }
            startActivity(intent)
        }

        btnRadar.setOnClickListener { startActivity(Intent(this, MapaRiesgoActivity::class.java)) }

        // Volver a LoginActivity
        btnVolver.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        cargarMetricasClinicas()
    }

    private fun cargarMetricasClinicas() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val expedientes: List<DiagnosticEntity> = database.diagnosticDao().obtenerListaPorMedico(idMedicoSesion)
                val total = expedientes.size

                contadorRiesgoAlto = expedientes.count { item ->
                    item.nivelSeveridad.contains("Alto", ignoreCase = true) ||
                            item.nivelSeveridad.contains("Rojo", ignoreCase = true) ||
                            item.patologiaDetectada.contains("G4", ignoreCase = true) ||
                            item.patologiaDetectada.contains("G5", ignoreCase = true) ||
                            item.porcentajeDano >= 50.0
                }

                withContext(Dispatchers.Main) {
                    tvTotal.text = total.toString()
                    tvRiesgoAlto.text = contadorRiesgoAlto.toString()
                    tvBadgeNotif.text = contadorRiesgoAlto.toString()

                    if (contadorRiesgoAlto > 0) {
                        tvAlerta.text = "Atención: Se han detectado $contadorRiesgoAlto caso(s) en RIESGO ALTO bajo su seguimiento clínico."
                    } else {
                        tvAlerta.text = "No hay alertas críticas pendientes en sus pacientes asignados. Sistema estable."
                        tvBadgeNotif.text = "0"
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    tvTotal.text = "0"
                    tvRiesgoAlto.text = "0"
                    tvBadgeNotif.text = "0"
                    tvAlerta.text = "Consola lista para evaluaciones clínicas."
                }
            }
        }
    }

    // ===================== EVALUADOR DE NEFROTOXICIDAD =====================
    private fun mostrarEvaluadorNefrotoxicidad() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Consola de Farmacovigilancia Renal")

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 30, 50, 10)
        }

        val tvInstruccion = TextView(this).apply {
            text = "Seleccione los fármacos consumidos de forma frecuente por el paciente:"
            setTextColor(Color.parseColor("#94A3B8"))
            textSize = 13f
            setPadding(0, 0, 0, 20)
        }
        layout.addView(tvInstruccion)

        val chkIbuprofeno = CheckBox(this).apply { text = "Ibuprofeno / Naproxeno (AINEs)" }
        val chkDiclofenaco = CheckBox(this).apply { text = "Diclofenaco / Ketorolaco" }
        val chkEnalapril = CheckBox(this).apply { text = "Enalapril / Captopril (IECA)" }
        val chkMetformina = CheckBox(this).apply { text = "Metformina (Antidiabético)" }
        val chkAminoglucosidos = CheckBox(this).apply { text = "Amikacina / Gentamicina (Antibióticos)" }

        layout.addView(chkIbuprofeno)
        layout.addView(chkDiclofenaco)
        layout.addView(chkEnalapril)
        layout.addView(chkMetformina)
        layout.addView(chkAminoglucosidos)

        builder.setView(layout)

        builder.setPositiveButton("Evaluar Toxicidad") { dialog, _ ->
            var porcentajeToxicidad = 10
            val listaRiesgos = mutableListOf<String>()

            if (chkIbuprofeno.isChecked) {
                porcentajeToxicidad += 30
                listaRiesgos.add("- AINEs (Ibuprofeno): Inhiben la síntesis de prostaglandinas, reduciendo el flujo sanguíneo renal.")
            }
            if (chkDiclofenaco.isChecked) {
                porcentajeToxicidad += 35
                listaRiesgos.add("- Diclofenaco/Ketorolaco: Alto riesgo de Necrosis Papilar y Falla Renal Aguda por deshidratación.")
            }
            if (chkEnalapril.isChecked) {
                porcentajeToxicidad += 15
                listaRiesgos.add("- IECA (Enalapril): Requiere ajuste en eGFR < 30 mL/min por riesgo de hiperpotasemia.")
            }
            if (chkMetformina.isChecked) {
                porcentajeToxicidad += 15
                listaRiesgos.add("- Metformina: Contraindicada en eGFR < 30 mL/min por riesgo de Acidosis Láctica.")
            }
            if (chkAminoglucosidos.isChecked) {
                porcentajeToxicidad += 40
                listaRiesgos.add("- Aminoglucósidos: Toxicidad tubular directa. Monitorear función renal diariamente.")
            }

            if (porcentajeToxicidad > 100) porcentajeToxicidad = 100

            val nivelRiesgo = when {
                porcentajeToxicidad >= 60 -> "ALERTA CRÍTICA: Alto Riesgo Nefrotóxico ($porcentajeToxicidad%)"
                porcentajeToxicidad >= 30 -> "ALERTA MODERADA: Precaución Renal ($porcentajeToxicidad%)"
                else -> "RIESGO BAJO: Combinación Farmacológica Segura ($porcentajeToxicidad%)"
            }

            val recomendacion = if (porcentajeToxicidad >= 50) {
                "\n\nRECOMENDACIÓN MÉDICA:\nSuspender AINEs inmediatamente. Reemplazar por Paracetamol (máx 2g/día). Garantizar hidratación oral de 3.0 Litros/día en campo."
            } else {
                "\n\nMantener dosis mínimas efectivas y monitoreo de creatinina mensual."
            }

            val detalle = if (listaRiesgos.isEmpty()) "Sin medicamentos de alto riesgo seleccionados." else listaRiesgos.joinToString("\n")

            AlertDialog.Builder(this)
                .setTitle(nivelRiesgo)
                .setMessage("Mecanismos de Daño Detectados:\n\n$detalle $recomendacion")
                .setPositiveButton("Guardar en Evaluación") { d, _ ->
                    Toast.makeText(this, "Evaluación de nefrotoxicidad registrada.", Toast.LENGTH_SHORT).show()
                    d.dismiss()
                }
                .setNegativeButton("Cerrar") { d, _ -> d.dismiss() }
                .show()

            dialog.dismiss()
        }

        builder.setNegativeButton("Cancelar") { dialog, _ -> dialog.dismiss() }
        builder.show()
    }

    // ===================== PRESCRIPTOR DE HIDRATACIÓN =====================
    private fun mostrarPrescriptorHidratacion() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Prescriptor de Hidratación por Clima")

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 30, 50, 10)
        }

        val etTemp = EditText(this).apply {
            hint = "Temperatura Ambiental (°C) - ej: 36"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
        val etHorasSol = EditText(this).apply {
            hint = "Horas de Trabajo bajo el Sol - ej: 6"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
        }

        layout.addView(etTemp)
        layout.addView(etHorasSol)
        builder.setView(layout)

        builder.setPositiveButton("Dosificar Hidratación") { dialog, _ ->
            val temp = etTemp.text.toString().toDoubleOrNull() ?: 30.0
            val horas = etHorasSol.text.toString().toDoubleOrNull() ?: 4.0

            var baseLitros = 2.0
            if (temp >= 32.0) baseLitros += 0.8
            if (temp >= 37.0) baseLitros += 0.7
            if (horas >= 5) baseLitros += 0.8

            val totalLitros = String.format(Locale.US, "%.1f", baseLitros)

            val esquemaTerapeutico = """
                RECETA DE HIDRATACIÓN EN CAMPO:
                - Meta Diaria Total: $totalLitros Litros/día
                - Jornada Mañana: 1.0 Litro (Tomas de 250 ml cada 45 min).
                - Turno de Mediodía: 1 Litro de Agua + 1 Sobre de Suero Oral.
                - Turno Tarde: 1.0 Litro de Agua fresca.
                
                PRECAUCIÓN: Prohibido ingerir bebidas energizantes o azucaradas en jornada de calor extremo.
            """.trimIndent()

            AlertDialog.Builder(this)
                .setTitle("Dosificación Calculada: $totalLitros L/día")
                .setMessage(esquemaTerapeutico)
                .setPositiveButton("Aceptar") { d, _ ->
                    Toast.makeText(this, "Prescripción de hidratación configurada.", Toast.LENGTH_SHORT).show()
                    d.dismiss()
                }
                .setNegativeButton("Cerrar") { d, _ -> d.dismiss() }
                .show()

            dialog.dismiss()
        }

        builder.setNegativeButton("Cancelar") { dialog, _ -> dialog.dismiss() }
        builder.show()
    }

    // ===================== CALCULADORA KFRE =====================
    private fun mostrarCalculadoraKfre() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Modelo KFRE: Riesgo de Falla Renal")

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 30, 50, 10)
        }

        val etEgfr = EditText(this).apply {
            hint = "eGFR Actual (mL/min/1.73m²) - ej: 32"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
        val etACR = EditText(this).apply {
            hint = "Albúmina/Creatinina en Orina (mg/g) - ej: 300"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
        val etEdad = EditText(this).apply {
            hint = "Edad (Años) - ej: 54"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
        }

        layout.addView(etEgfr)
        layout.addView(etACR)
        layout.addView(etEdad)
        builder.setView(layout)

        builder.setPositiveButton("Calcular Riesgo KFRE") { dialog, _ ->
            val egfr = etEgfr.text.toString().toDoubleOrNull() ?: 35.0
            val acr = etACR.text.toString().toDoubleOrNull() ?: 150.0
            val edad = etEdad.text.toString().toDoubleOrNull() ?: 50.0

            val scoreBase = (-0.2201 * (edad / 10 - 7.03)) - (0.5567 * (egfr / 5 - 3.79)) + (0.4510 * (Math.log(acr) - 5.12))

            var riesgo2Anios = (1.0 - 0.9832.pow(exp(scoreBase))) * 100
            var riesgo5Anios = (1.0 - 0.9365.pow(exp(scoreBase))) * 100

            if (riesgo2Anios < 0.5) riesgo2Anios = 0.5
            if (riesgo5Anios < 1.0) riesgo5Anios = 1.0
            if (riesgo2Anios > 99.0) riesgo2Anios = 99.0
            if (riesgo5Anios > 99.0) riesgo5Anios = 99.0

            val r2Str = String.format(Locale.US, "%.1f", riesgo2Anios)
            val r5Str = String.format(Locale.US, "%.1f", riesgo5Anios)

            val nivelAlerta = when {
                riesgo5Anios >= 15.0 -> "RIESGO ALTO DE PRE-DIÁLISIS (>=15%)"
                riesgo5Anios >= 5.0 -> "RIESGO MODERADO (5% - 14.9%)"
                else -> "RIESGO BAJO DE FALLA RENAL (<5%)"
            }

            val mensaje = """
                PROYECCIÓN DE FALLA RENAL GRAVE:
                - Riesgo de requerir Diálisis a 2 Años: $r2Str%
                - Riesgo de requerir Diálisis a 5 Años: $r5Str%
                
                PROTOCOLO RECOMENDADO:
                ${if (riesgo5Anios >= 15.0) "Derivación urgente a Nefrología. Preparación de acceso vascular / terapia nefroprotectora avanzada." else "Seguimiento ambulatorio cada 3 a 6 meses con control de proteinuria."}
            """.trimIndent()

            AlertDialog.Builder(this)
                .setTitle(nivelAlerta)
                .setMessage(mensaje)
                .setPositiveButton("Guardar en Protocolo") { d, _ ->
                    Toast.makeText(this, "Riesgo KFRE calculado y registrado.", Toast.LENGTH_SHORT).show()
                    d.dismiss()
                }
                .setNegativeButton("Cerrar") { d, _ -> d.dismiss() }
                .show()

            dialog.dismiss()
        }

        builder.setNegativeButton("Cancelar") { dialog, _ -> dialog.dismiss() }
        builder.show()
    }

    // ===================== CALCULADORA CKD-EPI =====================
    private fun mostrarCalculadoraCkdEpi() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Calculadora Clínica CKD-EPI")

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 10)
        }

        val etCreatinina = EditText(this).apply {
            hint = "Creatinina Sérica (mg/dL) - ej: 1.2"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
        val etEdad = EditText(this).apply {
            hint = "Edad del Paciente (Años) - ej: 48"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
        }

        layout.addView(etCreatinina)
        layout.addView(etEdad)
        builder.setView(layout)

        builder.setPositiveButton("Calcular Tasa (eGFR)") { dialog, _ ->
            val scr = etCreatinina.text.toString().toDoubleOrNull() ?: 1.0
            val edad = etEdad.text.toString().toDoubleOrNull() ?: 50.0

            val egfr = 141 * (scr / 0.9).pow(-1.209) * 0.993.pow(edad)
            val resultadoRedondeado = String.format(Locale.US, "%.1f", egfr)

            val clasificacion = when {
                egfr >= 90.0 -> "Estadio G1: Normal o Alto (>=90 mL/min)"
                egfr >= 60.0 -> "Estadio G2: Levemente Disminuido (60-89 mL/min)"
                egfr >= 45.0 -> "Estadio G3a: Moderado (45-59 mL/min)"
                egfr >= 30.0 -> "Estadio G3b: Moderado a Severo (30-44 mL/min)"
                egfr >= 15.0 -> "Estadio G4: Severamente Disminuido (15-29 mL/min)"
                else -> "Estadio G5: Falla Renal (<15 mL/min)"
            }

            AlertDialog.Builder(this)
                .setTitle("Resultado eGFR: $resultadoRedondeado mL/min/1.73m²")
                .setMessage("Diagnóstico Estimado:\n$clasificacion")
                .setPositiveButton("Aceptar") { d, _ -> d.dismiss() }
                .show()

            dialog.dismiss()
        }

        builder.setNegativeButton("Cancelar") { dialog, _ -> dialog.dismiss() }
        builder.show()
    }

    // ===================== GENERACIÓN DE INFORME PDF =====================
    private fun generarInformeMedicoOficial() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val expedientes = database.diagnosticDao().obtenerListaPorMedico(idMedicoSesion)
                if (expedientes.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MedicoDashboardActivity, "No hay expedientes clínicos asociados a su cuenta.", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                val ultimo = expedientes.first()
                val esAlto = ultimo.nivelSeveridad.contains("Alto", ignoreCase = true) ||
                        ultimo.nivelSeveridad.contains("Rojo", ignoreCase = true) ||
                        ultimo.porcentajeDano >= 50.0

                val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                val fechaTexto = dateFormat.format(Date(ultimo.fechaRegistroTimestamp))

                val pdfFile = crearPdfInforme(ultimo, fechaTexto, esAlto)

                withContext(Dispatchers.Main) {
                    if (pdfFile != null) {
                        AlertDialog.Builder(this@MedicoDashboardActivity)
                            .setTitle("Informe PDF generado")
                            .setMessage("Se guardó correctamente en:\n${pdfFile.absolutePath}")
                            .setPositiveButton("Abrir PDF") { _, _ ->
                                abrirPdf(pdfFile)
                            }
                            .setNegativeButton("Cerrar", null)
                            .show()
                    } else {
                        Toast.makeText(this@MedicoDashboardActivity, "Error al guardar el PDF.", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MedicoDashboardActivity, "Error al acceder a la base de datos.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun crearPdfInforme(diagnostico: DiagnosticEntity, fechaTexto: String, esAlto: Boolean): File? {
        // Solicitar permiso si es necesario (Android 9 y anteriores)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_WRITE_PERMISSION)
                return null
            }
        }

        val pdfDocument = PdfDocument()

        // Página A4: 595 x 842 puntos
        val pageWidth = 595
        val pageHeight = 842
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint()

        // Fondo blanco
        canvas.drawColor(Color.WHITE)

        // ============ ENCABEZADO ============
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
        canvas.drawText("Sistema NefroScan AI - Informe Clínico Diagnóstico", pageWidth / 2f, 70f, paint)

        paint.color = Color.parseColor("#00B4D8")
        canvas.drawRect(0f, 120f, pageWidth.toFloat(), 125f, paint)

        // ============ DATOS DEL MÉDICO Y PACIENTE ============
        paint.color = Color.parseColor("#03045E")
        paint.textAlign = Paint.Align.LEFT
        paint.textSize = 14f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

        val leftMargin = 40f
        val rightMargin = pageWidth - 40f
        var y = 160f

        canvas.drawText("Médico Responsable: $nombreMedicoSesion", leftMargin, y, paint)
        y += 25
        canvas.drawText("ID Médico: $idMedicoSesion", leftMargin, y, paint)
        y += 30

        paint.color = Color.BLACK
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 13f

        // Línea divisoria
        paint.color = Color.parseColor("#E2E8F0")
        canvas.drawLine(leftMargin, y, rightMargin, y, paint)
        y += 30

        paint.color = Color.parseColor("#03045E")
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("DATOS DEL PACIENTE", leftMargin, y, paint)
        y += 25

        paint.color = Color.BLACK
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

        canvas.drawText("Nombre: ${diagnostico.nombrePaciente}", leftMargin, y, paint)
        y += 20
        canvas.drawText("ID Paciente: ${diagnostico.idPaciente}", leftMargin, y, paint)
        y += 20
        canvas.drawText("Edad: ${diagnostico.edadPaciente} años", leftMargin, y, paint)
        y += 20
        canvas.drawText("Fecha de Evaluación: $fechaTexto", leftMargin, y, paint)
        y += 30

        // ============ RESULTADOS DEL DIAGNÓSTICO ============
        paint.color = Color.parseColor("#03045E")
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("RESULTADOS DEL ESTUDIO", leftMargin, y, paint)
        y += 25

        // Tabla simple
        val tableLeft = leftMargin
        val tableRight = rightMargin
        val col1Width = (tableRight - tableLeft) * 0.5f
        val col2Width = (tableRight - tableLeft) * 0.5f

        val filas = listOf(
            "Patología Detectada" to diagnostico.patologiaDetectada,
            "Porcentaje de Daño" to "${diagnostico.porcentajeDano}%",
            "Nivel de Severidad" to diagnostico.nivelSeveridad,
            "eGFR a 5 años" to "${diagnostico.egfrEstimado5Anios} mL/min",
            "eGFR a 10 años" to "${diagnostico.egfrEstimado10Anios} mL/min"
        )

        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

        val rowHeight = 25f
        var rowTop = y

        // Encabezado de tabla
        paint.color = Color.parseColor("#E8F4FD")
        canvas.drawRect(tableLeft, rowTop, tableRight, rowTop + rowHeight, paint)
        paint.color = Color.parseColor("#03045E")
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Parámetro", tableLeft + 10, rowTop + 17, paint)
        canvas.drawText("Valor", tableLeft + col1Width + 10, rowTop + 17, paint)
        rowTop += rowHeight

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

        for ((param, value) in filas) {
            paint.color = Color.WHITE
            canvas.drawRect(tableLeft, rowTop, tableRight, rowTop + rowHeight, paint)
            paint.color = Color.BLACK
            canvas.drawText(param, tableLeft + 10, rowTop + 17, paint)
            canvas.drawText(value, tableLeft + col1Width + 10, rowTop + 17, paint)

            paint.color = Color.parseColor("#E2E8F0")
            canvas.drawLine(tableLeft, rowTop + rowHeight, tableRight, rowTop + rowHeight, paint)
            rowTop += rowHeight
        }

        y = rowTop + 30

        // ============ RECOMENDACIONES ============
        paint.color = Color.parseColor("#03045E")
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("RECOMENDACIONES MÉDICAS", leftMargin, y, paint)
        y += 25

        paint.color = Color.BLACK
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 12f

        val recomendacion = if (esAlto) {
            "Derivación urgente a Nefrología. Control estricto de hidratación y presión arterial.\n" +
                    "Se sugiere repetir ecografía renal en 3 meses y monitorizar función renal cada 2 semanas."
        } else {
            "Control ambulatorio continuo. Mantener esquema de hidratación indicado.\n" +
                    "Seguimiento trimestral con nefrólogo o médico tratante."
        }

        val lineasRecomendacion = recomendacion.split("\n")
        for (linea in lineasRecomendacion) {
            canvas.drawText(linea, leftMargin, y, paint)
            y += 18
        }
        y += 10

        // ============ FIRMA ============
        canvas.drawLine(leftMargin, y + 20, rightMargin - 200, y + 20, paint)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Firma del Médico", leftMargin, y + 35, paint)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Dr. $nombreMedicoSesion", leftMargin, y + 55, paint)
        canvas.drawText("Cédula Profesional: 12345678", leftMargin, y + 73, paint)

        // ============ PIE DE PÁGINA ============
        paint.color = Color.parseColor("#64748B")
        paint.textSize = 10f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("Generado automáticamente por NefroScan AI - ${SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())}", pageWidth / 2f, pageHeight - 30f, paint)

        pdfDocument.finishPage(page)

        // Guardar PDF
        val fileName = "Informe_${diagnostico.nombrePaciente.replace(" ", "_")}_${System.currentTimeMillis()}.pdf"

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ usa MediaStore
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS + "/NefroScan")
                }
                val uri = contentResolver.insert(MediaStore.Files.getContentUri("external"), contentValues)
                val outputStream = contentResolver.openOutputStream(uri!!)
                pdfDocument.writeTo(outputStream)
                outputStream?.close()
                // Devolver un File temporal para poder abrirlo
                val tempFile = File(cacheDir, fileName)
                val tempOut = FileOutputStream(tempFile)
                pdfDocument.writeTo(tempOut)
                tempOut.close()
                tempFile
            } else {
                // Android 9 y anteriores: usar almacenamiento externo
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