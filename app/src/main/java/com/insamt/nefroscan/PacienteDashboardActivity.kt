package com.insamt.nefroscan

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
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

    // Clave para guardar los recordatorios de forma persistente en el celular
    private val PREFS_NAME = "NefroScanRecordatoriosPrefs"
    private val KEY_RECORDatorios = "lista_recordatorios_key"

    private val listaRecordatorios = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_paciente_dashboard)

        tvNombre = findViewById(R.id.tvNombrePacientePerfil)
        tvEdadSexo = findViewById(R.id.tvEdadPerfil)
        tvEstadio = findViewById(R.id.tvEstadioERCPerfil)

        val btnHistorial = findViewById<MaterialButton>(R.id.btnMiHistorial)
        val btnChatbot = findViewById<MaterialButton>(R.id.btnAsistenteIA)
        val btnPasaporte = findViewById<MaterialButton>(R.id.btnPasaporteQR)
        val btnGuia = findViewById<MaterialButton>(R.id.btnGuiaRenal)
        val btnVolver = findViewById<MaterialButton>(R.id.btnVolverRolesPaciente)

        // Referencias a los botones de herramientas de autocuidado
        val btnRecordatorio = findViewById<MaterialButton>(R.id.btnRecordatorioMedicamentos)
        val btnCalculadora = findViewById<MaterialButton>(R.id.btnCalculadoraHidratacion)
        val btnConsejos = findViewById<MaterialButton>(R.id.btnConsejosRapidos)
        val btnAyuda = findViewById<MaterialButton>(R.id.btnLineaAyuda)

        // Cargar recordatorios guardados previamente
        cargarRecordatoriosGuardados()

        cargarDatosFicha()

        btnHistorial.setOnClickListener {
            startActivity(Intent(this, HistorialActivity::class.java))
        }

        btnChatbot.setOnClickListener {
            startActivity(Intent(this, ChatbotActivity::class.java))
        }

        btnPasaporte.setOnClickListener {
            generarPasaporteQR()
        }

        btnGuia.setOnClickListener {
            startActivity(Intent(this, KidneyCareGuideActivity::class.java))
        }

        // Configuración de clics para las herramientas
        btnRecordatorio.setOnClickListener { mostrarDialogoRecordatorios() }
        btnCalculadora.setOnClickListener { mostrarCalculadoraHidratacion() }
        btnConsejos.setOnClickListener { mostrarConsejosRapidos() }
        btnAyuda.setOnClickListener { llamarLineaAyuda() }

        btnVolver.setOnClickListener { finish() }
    }

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

        val fechaFormateada = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(exp.fechaRegistroTimestamp))

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

    private fun crearBitmapQR(texto: String): Bitmap {
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

    // --- SISTEMA DE RECORDATORIOS PERSISTENTE Y REAL ---

    private fun cargarRecordatoriosGuardados() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val guardadosSet = prefs.getStringSet(KEY_RECORDatorios, null)
        listaRecordatorios.clear()
        if (guardadosSet != null) {
            listaRecordatorios.addAll(guardadosSet)
        } else {
            // Valores por defecto si es la primera vez
            listaRecordatorios.add("Losartán 50mg - 08:00 AM")
            listaRecordatorios.add("Eritropoyetina - 02:00 PM")
            guardarRecordatoriosEnPrefs()
        }
    }

    private fun guardarRecordatoriosEnPrefs() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putStringSet(KEY_RECORDatorios, listaRecordatorios.toSet()).apply()
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

        val itemsArray = listaRecordatorios.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Recordatorios de Medicamentos")
            .setItems(itemsArray) { _, which ->
                val seleccionado = itemsArray[which]
                // Opción para eliminar o gestionar el recordatorio seleccionado
                AlertDialog.Builder(this)
                    .setTitle("Gestionar Recordatorio")
                    .setMessage("¿Qué deseas hacer con:\n\n• $seleccionado?")
                    .setPositiveButton("Eliminar") { _, _ ->
                        listaRecordatorios.remove(seleccionado)
                        guardarRecordatoriosEnPrefs()
                        Toast.makeText(this, "Recordatorio eliminado", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("Cerrar", null)
                    .show()
            }
            .setPositiveButton("Agregar Nuevo") { _, _ ->
                mostrarFormularioNuevoRecordatorio()
            }
            .setNeutralButton("Cerrar", null)
            .show()
    }

    private fun mostrarFormularioNuevoRecordatorio() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 20, 40, 20)
        }

        val inputMedicamento = EditText(this).apply {
            hint = "Medicamento y Dosis (ej. Carbonato de Calcio 500mg)"
        }
        val inputHora = EditText(this).apply {
            hint = "Hora (ej. 08:00 PM)"
        }

        layout.addView(inputMedicamento)
        layout.addView(inputHora)

        AlertDialog.Builder(this)
            .setTitle("Nuevo Recordatorio Médico")
            .setView(layout)
            .setPositiveButton("Guardar y Programar") { _, _ ->
                val med = inputMedicamento.text.toString().trim()
                val hora = inputHora.text.toString().trim()
                if (med.isNotEmpty() && hora.isNotEmpty()) {
                    val nuevoItem = "$med - $hora"
                    listaRecordatorios.add(nuevoItem)
                    guardarRecordatoriosEnPrefs()

                    Toast.makeText(this, "¡Recordatorio guardado con éxito!", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "Debe completar ambos campos", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // --- DEMÁS HERRAMIENTAS DE AUTOCUIDADO ---

    private fun mostrarCalculadoraHidratacion() {
        val input = EditText(this).apply {
            hint = "Ingrese su peso en kg (ej. 70)"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        }

        AlertDialog.Builder(this)
            .setTitle("Calculadora de Hidratación")
            .setMessage("Ingrese su peso corporal para estimar el agua recomendada.\n\n⚠️ Nota: Los pacientes renales con restricción de líquidos deben seguir estrictamente la cuota indicada por su nefrólogo.")
            .setView(input)
            .setPositiveButton("Calcular") { _, _ ->
                val pesoStr = input.text.toString()
                val peso = pesoStr.toDoubleOrNull()
                if (peso != null && peso > 0) {
                    val aguaMl = peso * 30
                    AlertDialog.Builder(this)
                        .setTitle("Resultado Estimado")
                        .setMessage("Para un peso de $peso kg, su ingesta aproximada sugerida es de ${aguaMl.toInt()} ml diarios.")
                        .setPositiveButton("Aceptar", null)
                        .show()
                } else {
                    Toast.makeText(this, "Por favor ingrese un peso válido", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarConsejosRapidos() {
        val consejos = arrayOf(
            "1. Reduzca el consumo de sal para proteger sus riñones y controlar la presión arterial.",
            "2. Mantenga una hidratación adecuada según lo que su médico le haya autorizado.",
            "3. Evite el uso frecuente de analgésicos comunes (como ibuprofeno o naproxeno) sin receta.",
            "4. Controle regularmente su presión arterial y niveles de glucosa.",
            "5. Consuma porciones moderadas de proteínas y prefiera alimentos frescos."
        )

        AlertDialog.Builder(this)
            .setTitle("Consejos de Cuidado Renal")
            .setItems(consejos) { dialog, _ -> dialog.dismiss() }
            .setPositiveButton("Cerrar", null)
            .show()
    }

    private fun llamarLineaAyuda() {
        AlertDialog.Builder(this)
            .setTitle("Línea de Ayuda y Soporte")
            .setMessage("¿Desea comunicarse con la línea de atención de asistencia médica y soporte de NefroScan?")
            .setPositiveButton("Llamar") { _, _ ->
                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:132"))
                try {
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(this, "No se pudo abrir el marcador telefónico", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}