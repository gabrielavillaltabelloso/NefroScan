package com.insamt.nefroscan

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
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
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Suppress("SpellCheckingInspection")
class PacienteDashboardActivity : AppCompatActivity() {

    private lateinit var tvNombre: TextView
    private lateinit var tvEdad: TextView
    private lateinit var tvEstadio: TextView
    private val database: NefroScanDatabase by lazy { NefroScanDatabase.getDatabase(applicationContext) }
    private var ultimoExpediente: DiagnosticEntity? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_paciente_dashboard)

        tvNombre = findViewById(R.id.tvNombrePacientePerfil)
        tvEdad = findViewById(R.id.tvEdadPerfil)
        tvEstadio = findViewById(R.id.tvEstadioERCPerfil)

        val btnDetalles = findViewById<MaterialButton>(R.id.btnVerDetalles)
        val btnHistorial = findViewById<MaterialButton>(R.id.btnMiHistorial)
        val btnChatbot = findViewById<MaterialButton>(R.id.btnAsistenteIA)
        val btnPasaporte = findViewById<MaterialButton>(R.id.btnPasaporteQR)
        val btnGuia = findViewById<MaterialButton>(R.id.btnGuiaRenal)
        val btnVolver = findViewById<MaterialButton>(R.id.btnVolverRolesPaciente)

        // Botones de autocuidado
        val btnRecordatorio = findViewById<MaterialButton>(R.id.btnRecordatorioMedicamentos)
        val btnCalculadoraHidratacion = findViewById<MaterialButton>(R.id.btnCalculadoraHidratacion)
        val btnConsejos = findViewById<MaterialButton>(R.id.btnConsejosRapidos)
        val btnLineaAyuda = findViewById<MaterialButton>(R.id.btnLineaAyuda)

        cargarDatosFicha()

        btnDetalles.setOnClickListener { mostrarDetallesDiagnostico() }
        btnHistorial.setOnClickListener { startActivity(Intent(this, HistorialActivity::class.java)) }
        btnChatbot.setOnClickListener { startActivity(Intent(this, ChatbotActivity::class.java)) }
        btnPasaporte.setOnClickListener { generarPasaporteQR() }
        btnGuia.setOnClickListener { startActivity(Intent(this, KidneyCareGuideActivity::class.java)) }

        // Redirección segura para no cerrar la app al cambiar de perfil
        btnVolver.setOnClickListener {
            // Cambia 'MainActivity' por el nombre de tu pantalla de Roles o Login si es diferente
            val intent = Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
        }

        // Listeners de autocuidado
        btnRecordatorio.setOnClickListener { mostrarRecordatorios() }
        btnCalculadoraHidratacion.setOnClickListener { mostrarCalculadoraHidratacion() }
        btnConsejos.setOnClickListener { mostrarConsejosRapidos() }
        btnLineaAyuda.setOnClickListener { llamarLineaAyuda() }
    }

    private fun cargarDatosFicha() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val expedientes = database.diagnosticDao().obtenerTodosLista()
                if (expedientes.isNotEmpty()) {
                    ultimoExpediente = expedientes.first()
                    withContext(Dispatchers.Main) {
                        val exp = ultimoExpediente!!
                        tvNombre.text = exp.nombrePaciente ?: "Paciente Comunitario"
                        tvEdad.text = "Edad: ${exp.edadPaciente ?: "--"} años"
                        tvEstadio.text = "Estado: ${exp.nivelSeveridad ?: "En evaluación"}"
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        tvNombre.text = "Sin Expediente Guardado"
                        tvEdad.text = "Realice un tamizaje o ecografía previa"
                        tvEstadio.text = "Bajo Monitoreo General"
                    }
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    tvNombre.text = "Paciente NefroScan"
                    tvEdad.text = "Error al cargar datos"
                    tvEstadio.text = "Reintente más tarde"
                }
            }
        }
    }

    private fun mostrarDetallesDiagnostico() {
        val exp = ultimoExpediente
        if (exp == null) {
            Toast.makeText(this, "No hay expedientes guardados aún.", Toast.LENGTH_SHORT).show()
            return
        }

        val fechaFormateada = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            .format(Date(exp.fechaRegistroTimestamp))

        val mensaje = """
            Paciente: ${exp.nombrePaciente}
            Edad: ${exp.edadPaciente} años
            Patología: ${exp.patologiaDetectada}
            Severidad: ${exp.nivelSeveridad}
            Daño Tisular: ${exp.porcentajeDano}%
            eGFR 5 años: ${exp.egfrEstimado5Anios} mL/min
            eGFR 10 años: ${exp.egfrEstimado10Anios} mL/min
            Fecha de registro: $fechaFormateada
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("Último Diagnóstico")
            .setMessage(mensaje)
            .setPositiveButton("Cerrar") { d, _ -> d.dismiss() }
            .show()
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
            .setNegativeButton("Compartir") { _, _ -> compartirQR(bitmap, datosQR) }
            .show()
    }

    private fun compartirQR(bitmap: Bitmap, texto: String) {
        try {
            val cachePath = File(cacheDir, "qr_pasaporte.png")
            val outputStream = FileOutputStream(cachePath)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            outputStream.flush()
            outputStream.close()

            val uri = androidx.core.content.FileProvider.getUriForFile(
                this,
                "$packageName.fileprovider",
                cachePath
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, texto)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(shareIntent, "Compartir pasaporte clínico"))
        } catch (e: Exception) {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, texto)
            }
            startActivity(Intent.createChooser(shareIntent, "Compartir pasaporte clínico"))
        }
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

    // ==================== FUNCIONES DE AUTOCUIDADO ====================

    private fun mostrarRecordatorios() {
        val recordatorios = """
            • Toma tus medicamentos según lo indicado:
              - Antihipertensivos (si los tienes indicados)
              - Quelantes de fósforo (junto con las comidas)
              - Eritropoyetina (si está prescrita)
              - Suplementos de vitamina D
            
            • Tip: Programa alarmas fijas diarias en tu teléfono para no olvidarlos.
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("Recordatorio de Medicamentos")
            .setMessage(recordatorios)
            .setPositiveButton("Entendido") { d, _ -> d.dismiss() }
            .show()
    }

    private fun mostrarCalculadoraHidratacion() {
        val input = EditText(this).apply {
            hint = "Peso en kg (ej. 68)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        }

        AlertDialog.Builder(this)
            .setTitle("Calculadora de Hidratación")
            .setMessage("Ingresa tu peso actual para estimar la ingesta diaria de líquidos.")
            .setView(input)
            .setPositiveButton("Calcular") { _, _ ->
                val pesoStr = input.text.toString()
                if (pesoStr.isNotEmpty()) {
                    val peso = pesoStr.toFloatOrNull()
                    if (peso != null && peso > 0) {
                        val recomendacionBase = peso * 30
                        val recomendacionRenal = 500 + (peso * 5)
                        val mensaje = """
                            Peso ingresado: $peso kg
                            
                            • Recomendación general (sin ERC): ${recomendacionBase.toInt()} mL/día (~${"%.1f".format(recomendacionBase / 250)} vasos)
                            • Con enfermedad renal avanzada (restricción hídrica): ${recomendacionRenal.toInt()} mL/día
                            
                            ⚠️ Nota clínica: Si tienes edema o diálisis, consulta siempre a tu médico la cantidad exacta.
                        """.trimIndent()

                        AlertDialog.Builder(this)
                            .setTitle("Resultado de Hidratación")
                            .setMessage(mensaje)
                            .setPositiveButton("Cerrar") { d, _ -> d.dismiss() }
                            .show()
                    } else {
                        Toast.makeText(this, "Ingresa un peso válido", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Debe ingresar un peso", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar") { d, _ -> d.dismiss() }
            .show()
    }

    private fun mostrarConsejosRapidos() {
        val consejos = """
            • Dieta renal preventiva:
              - Reduce el sodio (evita sopas instantáneas y embutidos).
              - Modera alimentos altos en potasio (plátano, cítricos) si tu médico lo indicó.
              - Disminuye bebidas azucaradas y ultraprocesadas.
            
            • Hábitos saludables:
              - Realiza caminata ligera de 30 minutos al día.
              - Evita la automedicación con analgésicos tipo AINEs (Ibuprofeno, Diclofenaco).
            
            • Monitoreo regular:
              - Control de presión arterial semanal.
              - Examen de creatinina y proteinuria periódicos.
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("Consejos de Salud Renal")
            .setMessage(consejos)
            .setPositiveButton("Entendido") { d, _ -> d.dismiss() }
            .show()
    }

    private fun llamarLineaAyuda() {
        val telefono = "tel:132" +
                "" // Línea de orientación médica o de emergencia de referencia
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse(telefono))
        try {
            startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(this, "No se encontró una aplicación de llamadas", Toast.LENGTH_SHORT).show()
        }
    }
}