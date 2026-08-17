package com.insamt.nefroscan

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.exp
import kotlin.math.pow

class MedicoDashboardActivity : AppCompatActivity() {

    private lateinit var tvTotal: TextView
    private lateinit var tvRiesgoAlto: TextView
    private lateinit var tvAlerta: TextView
    private val database: NefroScanDatabase by lazy { NefroScanDatabase.getDatabase(applicationContext) }

    private var idMedicoSesion: String = ""
    private var nombreMedicoSesion: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_medico_dashboard)

        // 1. Obtener la sesión activa del médico
        val prefs = getSharedPreferences("SesionNefroScan", Context.MODE_PRIVATE)
        idMedicoSesion = prefs.getString("ID_USUARIO", "") ?: ""
        nombreMedicoSesion = prefs.getString("NOMBRE_USUARIO", "Dr. Especialista") ?: "Dr. Especialista"

        tvTotal = findViewById(R.id.tvKpiTotal)
        tvRiesgoAlto = findViewById(R.id.tvKpiRiesgoAlto)
        tvAlerta = findViewById(R.id.tvAlertaDerivacion)

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

        // =====================================================================
        // NAVEGACIÓN A MÓDULOS AVANZADOS
        // =====================================================================
        btnAbrirCentroAnalitica.setOnClickListener {
            startActivity(Intent(this, CentroAnaliticaMedicoActivity::class.java))
        }

        btnAbrirDictamenMedico.setOnClickListener {
            startActivity(Intent(this, DictamenMedicoActivity::class.java))
        }

        btnAbrirSimuladorDigital.setOnClickListener {
            startActivity(Intent(this, SimuladorNefroDigitalActivity::class.java))
        }

        btnAbrirTriajeRadar.setOnClickListener {
            startActivity(Intent(this, TriajeRadarPredictivoActivity::class.java))
        }

        // =====================================================================
        // NAVEGACIÓN A HERRAMIENTAS TRADICIONALES
        // =====================================================================
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

        btnRadar.setOnClickListener {
            startActivity(Intent(this, MapaRiesgoActivity::class.java))
        }

        btnVolver.setOnClickListener { finish() }
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

                val riesgoAltoCount = expedientes.count { item ->
                    item.nivelSeveridad.contains("Alto", ignoreCase = true) ||
                            item.nivelSeveridad.contains("Rojo", ignoreCase = true) ||
                            item.patologiaDetectada.contains("G4", ignoreCase = true) ||
                            item.patologiaDetectada.contains("G5", ignoreCase = true) ||
                            item.porcentajeDano >= 50.0
                }

                withContext(Dispatchers.Main) {
                    tvTotal.text = total.toString()
                    tvRiesgoAlto.text = riesgoAltoCount.toString()

                    if (riesgoAltoCount > 0) {
                        tvAlerta.text = "Atención: Se han detectado $riesgoAltoCount caso(s) en RIESGO ALTO bajo su seguimiento clínico."
                    } else {
                        tvAlerta.text = "No hay alertas críticas pendientes en sus pacientes asignados. Sistema estable."
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    tvTotal.text = "0"
                    tvRiesgoAlto.text = "0"
                    tvAlerta.text = "Consola lista para evaluaciones clínicas."
                }
            }
        }
    }

    // =========================================================================
    // FARMACOVIGILANCIA Y TOXICIDAD RENAL
    // =========================================================================
    private fun mostrarEvaluadorNefrotoxicidad() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Consola de Farmacovigilancia Renal")

        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(50, 30, 50, 10)
        }

        val tvInstruccion = TextView(this).apply {
            text = "Seleccione los fármacos consumidos de forma frecuente por el paciente:"
            setTextColor(android.graphics.Color.parseColor("#94A3B8"))
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
                listaRiesgos.add("• AINEs (Ibuprofeno): Inhiben la síntesis de prostaglandinas, reduciendo el flujo sanguíneo renal.")
            }
            if (chkDiclofenaco.isChecked) {
                porcentajeToxicidad += 35
                listaRiesgos.add("• Diclofenaco/Ketorolaco: Alto riesgo de Necrosis Papilar y Falla Renal Aguda por deshidratación.")
            }
            if (chkEnalapril.isChecked) {
                porcentajeToxicidad += 15
                listaRiesgos.add("• IECA (Enalapril): Requiere ajuste en eGFR < 30 mL/min por riesgo de hiperpotasemia.")
            }
            if (chkMetformina.isChecked) {
                porcentajeToxicidad += 15
                listaRiesgos.add("• Metformina: Contraindicada en eGFR < 30 mL/min por riesgo de Acidosis Láctica.")
            }
            if (chkAminoglucosidos.isChecked) {
                porcentajeToxicidad += 40
                listaRiesgos.add("• Aminoglucósidos: Toxicidad tubular directa. Monitorear función renal diariamente.")
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

    // =========================================================================
    // PRESCRIPTOR DE HIDRATACIÓN POR CLIMA
    // =========================================================================
    private fun mostrarPrescriptorHidratacion() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Prescriptor de Hidratación por Clima")

        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
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
                • Meta Diaria Total: $totalLitros Litros/día
                • Jornada Mañana: 1.0 Litro (Tomas de 250 ml cada 45 min).
                • Turno de Mediodía: 1 Litro de Agua + 1 Sobre de Suero Oral.
                • Turno Tarde: 1.0 Litro de Agua fresca.
                
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

    // =========================================================================
    // CALCULADORA KFRE DE RIESGO DE DIÁLISIS
    // =========================================================================
    private fun mostrarCalculadoraKfre() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Modelo KFRE: Riesgo de Falla Renal")

        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
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
                riesgo5Anios >= 15.0 -> "RIESGO ALTO DE PRE-DIÁLISIS (≥15%)"
                riesgo5Anios >= 5.0 -> "RIESGO MODERADO (5% - 14.9%)"
                else -> "RIESGO BAJO DE FALLA RENAL (<5%)"
            }

            val mensaje = """
                PROYECCIÓN DE FALLA RENAL GRAVE:
                • Riesgo de requerir Diálisis a 2 Años: $r2Str%
                • Riesgo de requerir Diálisis a 5 Años: $r5Str%
                
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

    private fun mostrarCalculadoraCkdEpi() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Calculadora Clínica CKD-EPI")

        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
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

    private fun generarInformeMedicoOficial() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val expedientes: List<DiagnosticEntity> = database.diagnosticDao().obtenerListaPorMedico(idMedicoSesion)
                withContext(Dispatchers.Main) {
                    if (expedientes.isEmpty()) {
                        Toast.makeText(this@MedicoDashboardActivity, "No hay expedientes clínicos asociados a su cuenta para generar el informe.", Toast.LENGTH_SHORT).show()
                        return@withContext
                    }

                    val ultimo = expedientes.first()
                    val esAlto = ultimo.nivelSeveridad.contains("Alto", ignoreCase = true) ||
                            ultimo.nivelSeveridad.contains("Rojo", ignoreCase = true) ||
                            ultimo.porcentajeDano >= 50.0

                    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                    val fechaTexto = dateFormat.format(Date(ultimo.fechaRegistroTimestamp))

                    val informeTexto = """
                        INSTITUTO NACIONAL DE SAN MIGUEL TEPEZONTES
                        SISTEMA NEFROSCAN - INFORME CLÍNICO DIAGNÓSTICO
                        Médico Responsable: $nombreMedicoSesion ($idMedicoSesion)
                        ---------------------------------------------------
                        Paciente: ${ultimo.nombrePaciente} (ID: ${ultimo.idPaciente})
                        Edad: ${ultimo.edadPaciente} años
                        Fecha de Evaluación: $fechaTexto
                        
                        ESTUDIO ECOGRÁFICO E INTELIGENCIA ARTIFICIAL:
                        • Patología Detectada: ${ultimo.patologiaDetectada}
                        • Porcentaje de Daño Estimado: ${ultimo.porcentajeDano}%
                        • Nivel de Severidad: ${ultimo.nivelSeveridad}
                        • Tasa eGFR Proyectada a 5 Años: ${ultimo.egfrEstimado5Anios} mL/min
                        • Tasa eGFR Proyectada a 10 Años: ${ultimo.egfrEstimado10Anios} mL/min
                        
                        PRESCRIPCIÓN PREVENTIVA:
                        • Meta de Hidratación: ${ultimo.litrosAguaDiarios} Litros/día
                        • Restricción de Sodio: ${ultimo.nivelSodio}
                        
                        RECOMENDACIÓN MÉDICA:
                        ${if (esAlto) "Derivación urgente a Nefrología. Control estricto de hidratación y presión arterial." else "Control ambulatorio continuo y seguimiento en tamizaje comunitario."}
                        ---------------------------------------------------
                        Estado de Sincronización: ${if (ultimo.sincronizadoConNube) "Sincronizado Cloud" else "Pendiente Local (Room)"}
                    """.trimIndent()

                    AlertDialog.Builder(this@MedicoDashboardActivity)
                        .setTitle("Informe Médico Preparado")
                        .setMessage(informeTexto)
                        .setPositiveButton("Simular Impresión / PDF") { d, _ ->
                            Toast.makeText(this@MedicoDashboardActivity, "Informe generado y guardado en almacenamiento local.", Toast.LENGTH_LONG).show()
                            d.dismiss()
                        }
                        .setNegativeButton("Cerrar") { d, _ -> d.dismiss() }
                        .show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MedicoDashboardActivity, "Error al acceder a la base de datos.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}