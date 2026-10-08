package com.insamt.nefroscan

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Suppress("SpellCheckingInspection")
class PacienteDashboardActivity : AppCompatActivity() {

    private lateinit var tvNombre: TextView
    private lateinit var tvEdadSexo: TextView
    private lateinit var tvEstadio: TextView
    private val database: NefroScanDatabase by lazy { NefroScanDatabase.getDatabase(applicationContext) }
    private var ultimoExpediente: DiagnosticEntity? = null

    private val PREFS_NAME = "NefroScanRecordatoriosPrefs"
    private val KEY_RECORDATORIOS = "lista_recordatorios_json"
    private val listaRecordatorios = mutableListOf<Recordatorio>()

    // Data class para recordatorios
    data class Recordatorio(
        val id: Int,
        val medicamento: String,
        val dosis: String,
        val hora: Int,
        val minuto: Int,
        val activo: Boolean = true
    ) {
        fun horaFormateada(): String {
            val amPm = if (hora < 12) "AM" else "PM"
            val hora12 = when {
                hora == 0 -> 12
                hora > 12 -> hora - 12
                else -> hora
            }
            return String.format(Locale.getDefault(), "%02d:%02d %s", hora12, minuto, amPm)
        }

        fun toJson(): JSONObject = JSONObject().apply {
            put("id", id)
            put("medicamento", medicamento)
            put("dosis", dosis)
            put("hora", hora)
            put("minuto", minuto)
            put("activo", activo)
        }

        companion object {
            fun fromJson(json: JSONObject): Recordatorio = Recordatorio(
                id = json.getInt("id"),
                medicamento = json.getString("medicamento"),
                dosis = json.optString("dosis", ""),
                hora = json.getInt("hora"),
                minuto = json.getInt("minuto"),
                activo = json.optBoolean("activo", true)
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_paciente_dashboard)

        tvNombre = findViewById(R.id.tvNombrePacientePerfil)
        tvEdadSexo = findViewById(R.id.tvEdadPerfil)
        tvEstadio = findViewById(R.id.tvEstadioERCPerfil)

        val btnVerDetalles = findViewById<MaterialButton>(R.id.btnVerDetalles)
        val btnHistorial = findViewById<MaterialButton>(R.id.btnMiHistorial)
        val btnChatbot = findViewById<MaterialButton>(R.id.btnAsistenteIA)
        val btnPasaporte = findViewById<MaterialButton>(R.id.btnPasaporteQR)
        val btnGuia = findViewById<MaterialButton>(R.id.btnGuiaRenal)
        val btnVolver = findViewById<MaterialButton>(R.id.btnVolverRolesPaciente)

        val btnRecordatorio = findViewById<MaterialButton>(R.id.btnRecordatorioMedicamentos)
        val btnCalculadora = findViewById<MaterialButton>(R.id.btnCalculadoraHidratacion)
        val btnConsejos = findViewById<MaterialButton>(R.id.btnConsejosRapidos)
        val btnAyuda = findViewById<MaterialButton>(R.id.btnLineaAyuda)

        cargarRecordatoriosGuardados()
        cargarDatosFicha()

        // Listener para Ver Detalles del Último Diagnóstico
        btnVerDetalles.setOnClickListener {
            mostrarDetallesUltimoDiagnostico()
        }

        btnHistorial.setOnClickListener {
            startActivity(Intent(this, HistorialActivity::class.java))
        }

        btnChatbot.setOnClickListener {
            startActivity(Intent(this, ChatbotActivity::class.java))
        }

        btnPasaporte.setOnClickListener { generarPasaporteQR() }

        btnGuia.setOnClickListener {
            startActivity(Intent(this, KidneyCareGuideActivity::class.java))
        }

        btnRecordatorio.setOnClickListener { mostrarDialogoRecordatorios() }
        btnCalculadora.setOnClickListener { mostrarCalculadoraHidratacion() }
        btnConsejos.setOnClickListener { mostrarConsejosRapidos() }
        btnAyuda.setOnClickListener { llamarLineaAyuda() }

        btnVolver.setOnClickListener { finish() }
    }

    // ═══════════════════════════════════════════════════════════════
    // 1. VER DETALLES DEL ÚLTIMO DIAGNÓSTICO
    // ═══════════════════════════════════════════════════════════════

    private fun mostrarDetallesUltimoDiagnostico() {
        val exp = ultimoExpediente
        if (exp == null) {
            AlertDialog.Builder(this)
                .setTitle("Sin Datos")
                .setMessage("No hay un diagnóstico registrado todavía.\n\nRealice primero un análisis de orina para generar su expediente.")
                .setPositiveButton("Entendido", null)
                .show()
            return
        }

        val fechaFormateada = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            .format(Date(exp.fechaRegistroTimestamp))

        val emoji = when {
            exp.nivelSeveridad.contains("Crón", true) ||
                    exp.nivelSeveridad.contains("Avanzad", true) -> "🔴"
            exp.nivelSeveridad.contains("Moderad", true) -> "🟠"
            exp.nivelSeveridad.contains("Leve", true) -> "🟡"
            else -> "🟢"
        }

        val mensaje = """
            👤 Paciente: ${exp.nombrePaciente}
            🎂 Edad: ${exp.edadPaciente} años

            ─────────────────────────────
            🔬 PATOLOGÍA DETECTADA
            ${exp.patologiaDetectada}

            📊 Severidad: $emoji ${exp.nivelSeveridad}
            🧬 Daño tisular estimado: ${exp.porcentajeDano}%

            ─────────────────────────────
            📈 PROYECCIÓN DE FUNCIÓN RENAL
            • eGFR a 5 años:  ${exp.egfrEstimado5Anios} mL/min
            • eGFR a 10 años: ${exp.egfrEstimado10Anios} mL/min

            ─────────────────────────────
            📅 Fecha del registro: $fechaFormateada
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("📋 Detalles del Último Diagnóstico")
            .setMessage(mensaje)
            .setPositiveButton("Cerrar", null)
            .setNeutralButton("Ver Historial") { _, _ ->
                startActivity(Intent(this, HistorialActivity::class.java))
            }
            .show()
    }

    // ═══════════════════════════════════════════════════════════════
    // 2. SISTEMA DE RECORDATORIOS CON ALARMAS REALES
    // ═══════════════════════════════════════════════════════════════

    private fun cargarRecordatoriosGuardados() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonString = prefs.getString(KEY_RECORDATORIOS, null)
        listaRecordatorios.clear()

        if (jsonString != null) {
            try {
                val jsonArray = JSONArray(jsonString)
                for (i in 0 until jsonArray.length()) {
                    listaRecordatorios.add(Recordatorio.fromJson(jsonArray.getJSONObject(i)))
                }
            } catch (_: Exception) { /* ignorar */ }
        }

        if (listaRecordatorios.isEmpty()) {
            listaRecordatorios.add(Recordatorio(1, "Losartán", "50mg", 8, 0))
            listaRecordatorios.add(Recordatorio(2, "Eritropoyetina", "Según indicación", 14, 0))
            guardarRecordatoriosEnPrefs()
        }
    }

    private fun guardarRecordatoriosEnPrefs() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonArray = JSONArray()
        listaRecordatorios.forEach { jsonArray.put(it.toJson()) }
        prefs.edit().putString(KEY_RECORDATORIOS, jsonArray.toString()).apply()
    }

    private fun mostrarDialogoRecordatorios() {
        if (listaRecordatorios.isEmpty()) {
            AlertDialog.Builder(this)
                .setTitle("Mis Recordatorios")
                .setMessage("No tienes recordatorios activos actualmente.")
                .setPositiveButton("Agregar Nuevo") { _, _ -> mostrarFormularioNuevoRecordatorio() }
                .setNegativeButton("Cerrar", null)
                .show()
            return
        }

        val itemsArray = listaRecordatorios.map {
            "${if (it.activo) "✅" else "⏸"} ${it.medicamento} ${it.dosis} — ${it.horaFormateada()}"
        }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("💊 Recordatorios de Medicamentos")
            .setItems(itemsArray) { _, which ->
                gestionarRecordatorio(listaRecordatorios[which])
            }
            .setPositiveButton("➕ Agregar") { _, _ -> mostrarFormularioNuevoRecordatorio() }
            .setNeutralButton("Cerrar", null)
            .show()
    }

    private fun gestionarRecordatorio(recordatorio: Recordatorio) {
        val opciones = arrayOf(
            if (recordatorio.activo) "⏸ Desactivar" else "▶ Activar",
            "✏️ Editar",
            "🗑 Eliminar"
        )

        AlertDialog.Builder(this)
            .setTitle("Gestionar: ${recordatorio.medicamento}")
            .setItems(opciones) { _, which ->
                when (which) {
                    0 -> {
                        val index = listaRecordatorios.indexOfFirst { it.id == recordatorio.id }
                        if (index >= 0) {
                            listaRecordatorios[index] = recordatorio.copy(activo = !recordatorio.activo)
                            guardarRecordatoriosEnPrefs()
                            if (recordatorio.activo) {
                                cancelarAlarma(recordatorio)
                            } else {
                                programarAlarma(recordatorio)
                            }
                            Toast.makeText(
                                this,
                                if (recordatorio.activo) "Recordatorio desactivado" else "Recordatorio activado",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    1 -> mostrarFormularioNuevoRecordatorio(recordatorio)
                    2 -> {
                        listaRecordatorios.removeAll { it.id == recordatorio.id }
                        guardarRecordatoriosEnPrefs()
                        cancelarAlarma(recordatorio)
                        Toast.makeText(this, "Recordatorio eliminado", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarFormularioNuevoRecordatorio(existente: Recordatorio? = null) {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 30, 50, 30)
        }

        val inputMedicamento = EditText(this).apply {
            hint = "Medicamento (ej. Carbonato de Calcio)"
            setText(existente?.medicamento ?: "")
        }
        val inputDosis = EditText(this).apply {
            hint = "Dosis (ej. 500mg, 1 tableta)"
            setText(existente?.dosis ?: "")
        }
        val inputHora = EditText(this).apply {
            hint = "Hora en formato 24h (ej. 08:30, 20:00)"
            inputType = InputType.TYPE_CLASS_DATETIME
            setText(existente?.let { String.format(Locale.getDefault(), "%02d:%02d", it.hora, it.minuto) } ?: "")
        }

        layout.addView(inputMedicamento)
        layout.addView(inputDosis)
        layout.addView(inputHora)

        AlertDialog.Builder(this)
            .setTitle(if (existente == null) "➕ Nuevo Recordatorio" else "✏️ Editar Recordatorio")
            .setView(layout)
            .setPositiveButton("Guardar") { _, _ ->
                val med = inputMedicamento.text.toString().trim()
                val dosis = inputDosis.text.toString().trim()
                val horaStr = inputHora.text.toString().trim()

                if (med.isEmpty() || horaStr.isEmpty()) {
                    Toast.makeText(this, "Complete medicamento y hora", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val partes = horaStr.split(":")
                val h = partes.getOrNull(0)?.toIntOrNull()
                val m = partes.getOrNull(1)?.toIntOrNull() ?: 0

                if (h == null || h !in 0..23 || m !in 0..59) {
                    Toast.makeText(this, "Hora inválida. Use formato 24h (ej. 08:30)", Toast.LENGTH_LONG).show()
                    return@setPositiveButton
                }

                if (existente == null) {
                    val nuevoId = (listaRecordatorios.maxOfOrNull { it.id } ?: 0) + 1
                    val nuevo = Recordatorio(nuevoId, med, dosis, h, m)
                    listaRecordatorios.add(nuevo)
                    programarAlarma(nuevo)
                } else {
                    val index = listaRecordatorios.indexOfFirst { it.id == existente.id }
                    if (index >= 0) {
                        cancelarAlarma(existente)
                        val actualizado = existente.copy(medicamento = med, dosis = dosis, hora = h, minuto = m)
                        listaRecordatorios[index] = actualizado
                        programarAlarma(actualizado)
                    }
                }
                guardarRecordatoriosEnPrefs()
                Toast.makeText(this, "✅ Recordatorio guardado y programado", Toast.LENGTH_LONG).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun programarAlarma(recordatorio: Recordatorio) {
        if (!recordatorio.activo) return

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, RecordatorioReceiver::class.java).apply {
            putExtra("medicamento", recordatorio.medicamento)
            putExtra("dosis", recordatorio.dosis)
            putExtra("id", recordatorio.id)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            recordatorio.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, recordatorio.hora)
            set(Calendar.MINUTE, recordatorio.minuto)
            set(Calendar.SECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent
                )
            }
        } catch (_: SecurityException) {
            alarmManager.set(
                AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent
            )
        }
    }

    private fun cancelarAlarma(recordatorio: Recordatorio) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, RecordatorioReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            recordatorio.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    // ═══════════════════════════════════════════════════════════════
    // 3. CALCULADORA DE HIDRATACIÓN MEJORADA
    // ═══════════════════════════════════════════════════════════════

    private fun mostrarCalculadoraHidratacion() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 30, 50, 30)
        }

        val inputPeso = EditText(this).apply {
            hint = "Peso en kg (ej. 70)"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        }

        val inputEstadio = EditText(this).apply {
            hint = "Estadio ERC (1-5, o deje vacío si no sabe)"
            inputType = InputType.TYPE_CLASS_NUMBER
        }

        val tvNota = TextView(this).apply {
            text = "ℹ️ La fórmula se ajusta según el estadio de ERC:\n" +
                    "• Estadio 1-2: 35 ml/kg/día\n" +
                    "• Estadio 3: 30 ml/kg/día\n" +
                    "• Estadio 4: 25 ml/kg/día\n" +
                    "• Estadio 5 / Diálisis: Según prescripción médica"
            textSize = 12f
            setPadding(0, 16, 0, 16)
            setTextColor(Color.parseColor("#0077B6"))
        }

        layout.addView(inputPeso)
        layout.addView(inputEstadio)
        layout.addView(tvNota)

        AlertDialog.Builder(this)
            .setTitle("💧 Calculadora de Hidratación")
            .setView(layout)
            .setPositiveButton("Calcular") { _, _ ->
                val peso = inputPeso.text.toString().toDoubleOrNull()
                val estadio = inputEstadio.text.toString().toIntOrNull()

                if (peso == null || peso <= 0 || peso > 300) {
                    Toast.makeText(this, "Ingrese un peso válido (1-300 kg)", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val (mlPorKg, recomendacion) = when (estadio) {
                    1, 2 -> 35.0 to "Estadio temprano: hidratación normal recomendada."
                    3 -> 30.0 to "Estadio moderado: ligera restricción."
                    4 -> 25.0 to "Estadio avanzado: restricción moderada."
                    5 -> 20.0 to "⚠️ Estadio 5: consulte estrictamente a su nefrólogo."
                    else -> 30.0 to "Estadio no especificado: usando valor promedio."
                }

                val aguaMl = peso * mlPorKg
                val vasos = (aguaMl / 250).toInt()

                val resultado = """
                    📊 RESULTADO DE HIDRATACIÓN

                    ⚖️ Peso: $peso kg
                    ${if (estadio != null) "🏥 Estadio ERC: $estadio" else "🏥 Estadio: No especificado"}

                    💧 Ingesta diaria sugerida:
                    ${aguaMl.toInt()} ml  (≈ $vasos vasos de 250 ml)

                    📝 $recomendacion

                    ⚠️ IMPORTANTE: Esta es una estimación general.
                    Su nefrólogo puede indicar una cuota diferente según
                    su función renal, diuresis y medicación.
                """.trimIndent()

                AlertDialog.Builder(this)
                    .setTitle("Resultado")
                    .setMessage(resultado)
                    .setPositiveButton("Aceptar", null)
                    .setNeutralButton("Compartir") { _, _ ->
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, resultado)
                        }
                        startActivity(Intent.createChooser(shareIntent, "Compartir resultado"))
                    }
                    .show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // ═══════════════════════════════════════════════════════════════
    // 4. CONSEJOS RÁPIDOS POR CATEGORÍAS
    // ═══════════════════════════════════════════════════════════════

    private fun mostrarConsejosRapidos() {
        val categorias = arrayOf(
            "🥗 Alimentación",
            "💊 Medicamentos",
            "💧 Hidratación",
            "🏃 Estilo de Vida",
            "🩺 Signos de Alerta"
        )

        AlertDialog.Builder(this)
            .setTitle("💡 Consejos de Cuidado Renal")
            .setItems(categorias) { _, which ->
                when (which) {
                    0 -> mostrarConsejoCategoria(
                        "🥗 Alimentación Saludable",
                        arrayOf(
                            "1. Reduzca el consumo de sal a menos de 5 g diarios para proteger sus riñones y controlar la presión.",
                            "2. Prefiera alimentos frescos sobre procesados (embutidos, enlatados, snacks).",
                            "3. Modere las proteínas: prefiera pescado, pollo sin piel y legumbres.",
                            "4. Evite alimentos ricos en potasio si su médico lo indica (banano, naranja, tomate).",
                            "5. Controle el fósforo: limite lácteos, refrescos de cola y frutos secos en exceso.",
                            "6. Use hierbas y especias naturales en lugar de sal para sazonar."
                        )
                    )
                    1 -> mostrarConsejoCategoria(
                        "💊 Uso Correcto de Medicamentos",
                        arrayOf(
                            "1. NUNCA suspenda ni modifique dosis sin consultar a su nefrólogo.",
                            "2. Evite analgésicos como ibuprofeno, naproxeno o diclofenaco (dañan el riñón).",
                            "3. Para dolor leve prefiera acetaminofén según indicación médica.",
                            "4. Tenga cuidado con suplementos 'naturales' sin supervisión médica.",
                            "5. Use pastillero semanal y alarmas para no olvidar dosis.",
                            "6. Informe a todos sus médicos sobre los medicamentos que toma."
                        )
                    )
                    2 -> mostrarConsejoCategoria(
                        "💧 Hidratación Adecuada",
                        arrayOf(
                            "1. La cantidad de agua depende de su estadio de ERC: consulte su cuota.",
                            "2. En estadios avanzados o diálisis, respete estrictamente el límite indicado.",
                            "3. Distribuya el agua a lo largo del día, no toda de una vez.",
                            "4. Controle su peso diario: aumentos rápidos pueden indicar retención de líquidos.",
                            "5. Evite bebidas azucaradas y con alto contenido de fósforo (colas).",
                            "6. Use vasos pequeños para sensación de saciedad."
                        )
                    )
                    3 -> mostrarConsejoCategoria(
                        "🏃 Estilo de Vida Saludable",
                        arrayOf(
                            "1. Realice actividad física moderada al menos 30 min, 5 días por semana.",
                            "2. Caminar, nadar o bicicleta estática son excelentes opciones.",
                            "3. Controle su presión arterial en casa y anote los valores.",
                            "4. Mantenga niveles adecuados de glucosa si tiene diabetes.",
                            "5. Duerma 7-8 horas diarias para favorecer la recuperación renal.",
                            "6. Evite el tabaco y limite el consumo de alcohol.",
                            "7. Mantenga un peso saludable según su talla."
                        )
                    )
                    4 -> mostrarConsejoCategoria(
                        "🩺 Signos de Alerta — Acuda al Médico",
                        arrayOf(
                            "⚠️ Hinchazón repentina de pies, tobillos, manos o cara.",
                            "⚠️ Disminución notable en la cantidad de orina.",
                            "⚠️ Orina con espuma persistente o sangre.",
                            "⚠️ Fatiga extrema, náuseas o vómitos sin causa aparente.",
                            "⚠️ Dificultad para respirar o dolor en el pecho.",
                            "⚠️ Presión arterial muy alta que no baja con medicación.",
                            "⚠️ Confusión, somnolencia o calambres musculares.",
                            "🚨 Ante cualquiera de estos signos, ACUDA DE INMEDIATO a urgencias."
                        )
                    )
                }
            }
            .setPositiveButton("Cerrar", null)
            .show()
    }

    private fun mostrarConsejoCategoria(titulo: String, consejos: Array<String>) {
        AlertDialog.Builder(this)
            .setTitle(titulo)
            .setItems(consejos) { _, which ->
                AlertDialog.Builder(this)
                    .setTitle(titulo)
                    .setMessage(consejos[which])
                    .setPositiveButton("Entendido", null)
                    .setNeutralButton("Compartir") { _, _ ->
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "${titulo}\n\n${consejos[which]}\n\n— NefroScan")
                        }
                        startActivity(Intent.createChooser(shareIntent, "Compartir consejo"))
                    }
                    .show()
            }
            .setNegativeButton("Volver", null)
            .show()
    }

    // ═══════════════════════════════════════════════════════════════
    // RESTO DE FUNCIONES
    // ═══════════════════════════════════════════════════════════════

    private fun cargarDatosFicha() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val expedientes = database.diagnosticDao().obtenerTodosLista()
                if (expedientes.isNotEmpty()) {
                    ultimoExpediente = expedientes.first()
                    withContext(Dispatchers.Main) {
                        tvNombre.text = ultimoExpediente?.nombrePaciente ?: "Paciente Comunitario"
                        tvEdadSexo.text = "Edad: ${ultimoExpediente?.edadPaciente ?: "--"} años"
                        tvEstadio.text = "Estado: ${ultimoExpediente?.nivelSeveridad ?: "En evaluación"}"
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        tvNombre.text = "Sin Expediente Guardado"
                        tvEdadSexo.text = "Edad: --"
                        tvEstadio.text = "Estado: Bajo Monitoreo General"
                    }
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    tvNombre.text = "Paciente NefroScan"
                    tvEdadSexo.text = "Edad: --"
                    tvEstadio.text = "Estado: Evaluando datos..."
                }
            }
        }
    }

    private fun generarPasaporteQR() {
        val exp = ultimoExpediente
        if (exp == null) {
            Toast.makeText(this, "No hay expedientes locales registrados para generar el QR.", Toast.LENGTH_SHORT).show()
            return
        }

        val fechaFormateada = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            .format(Date(exp.fechaRegistroTimestamp))

        val datosQR = """
            --- PASAPORTE NEFROSCAN ---
            Paciente: ${exp.nombrePaciente}
            Edad: ${exp.edadPaciente} años
            Patología: ${exp.patologiaDetectada}
            Severidad: ${exp.nivelSeveridad}
            Daño Tisular: ${exp.porcentajeDano}%
            eGFR 5y: ${exp.egfrEstimado5Anios} mL/min
            eGFR 10y: ${exp.egfrEstimado10Anios} mL/min
            Fecha: $fechaFormateada
        """.trimIndent()

        val bitmap = crearBitmapQR(datosQR)
        val dialogView = layoutInflater.inflate(R.layout.dialog_tarjeta_qr, null)
        val ivQR = dialogView.findViewById<ImageView>(R.id.ivQrCodeDialog)
        val tvInfo = dialogView.findViewById<TextView>(R.id.tvInfoQrDialog)

        ivQR.setImageBitmap(bitmap)
        tvInfo.text = "Paciente: ${exp.nombrePaciente}\nSeveridad: ${exp.nivelSeveridad}"

        AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("Cerrar") { d, _ -> d.dismiss() }
            .show()
    }

    private fun crearBitmapQR(texto: String): Bitmap    {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(texto, BarcodeFormat.QR_CODE, 512, 512)
        val bitmap = createBitmap(512, 512, Bitmap.Config.RGB_565)
        for (x in 0 until 512) {
            for (y in 0 until 512) {
                bitmap[x, y] = if (bitMatrix[x, y]) Color.BLACK else Color.WHITE
            }
        }
        return bitmap
    }

    private fun llamarLineaAyuda() {
        AlertDialog.Builder(this)
            .setTitle("📞 Línea de Ayuda y Soporte")
            .setMessage("¿Desea comunicarse con la línea de atención de asistencia médica y soporte de NefroScan?\n\n📱 Línea: 132")
            .setPositiveButton("Llamar") { _, _ ->
                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:132"))
                try {
                    startActivity(intent)
                } catch (_: Exception) {
                    Toast.makeText(this, "No se pudo abrir el marcador telefónico", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}