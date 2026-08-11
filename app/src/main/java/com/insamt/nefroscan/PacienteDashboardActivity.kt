package com.insamt.nefroscan

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Suppress("SpellCheckingInspection")
class PacienteDashboardActivity : AppCompatActivity() {

    private lateinit var tvNombre: TextView
    private lateinit var tvEdadSexo: TextView
    private lateinit var tvEstadio: TextView
    private val database: NefroScanDatabase by lazy { NefroScanDatabase.getDatabase(applicationContext) }
    private var ultimoExpediente: DiagnosticEntity? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_paciente_dashboard)

        tvNombre = findViewById(R.id.tvNombrePacientePerfil)
        tvEdadSexo = findViewById(R.id.tvEdadSexoPerfil)
        tvEstadio = findViewById(R.id.tvEstadioERCPerfil)

        val btnHistorial = findViewById<MaterialButton>(R.id.btnMiHistorial)
        val btnChatbot = findViewById<MaterialButton>(R.id.btnAsistenteIA)
        val btnPasaporte = findViewById<MaterialButton>(R.id.btnPasaporteQR)
        val btnGuia = findViewById<MaterialButton>(R.id.btnGuiaRenal)
        val btnVolver = findViewById<MaterialButton>(R.id.btnVolverRolesPaciente)

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

        btnVolver.setOnClickListener { finish() }
    }

    private fun cargarDatosFicha() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val expedientes = database.diagnosticDao().obtenerTodos()
                if (expedientes.isNotEmpty()) {
                    ultimoExpediente = expedientes.last()
                    withContext(Dispatchers.Main) {
                        tvNombre.text = ultimoExpediente?.nombrePaciente ?: "Paciente Comunitario"
                        tvEdadSexo.text = "Edad: ${ultimoExpediente?.edadPaciente ?: "--"} años"
                        tvEstadio.text = "Estado: ${ultimoExpediente?.nivelSeveridad ?: "En evaluación"}"
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        tvNombre.text = "Sin Expediente Guardado"
                        tvEdadSexo.text = "Realice un tamizaje o ecografía previa"
                        tvEstadio.text = "Bajo Monitoreo General"
                    }
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    tvNombre.text = "Paciente NefroScan"
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
}