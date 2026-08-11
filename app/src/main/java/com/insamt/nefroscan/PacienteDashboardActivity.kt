package com.insamt.nefroscan

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

        val btnHistorial = findViewById<Button>(R.id.btnMiHistorial)
        val btnChatbot = findViewById<Button>(R.id.btnAsistenteIA)
        val btnPasaporte = findViewById<Button>(R.id.btnPasaporteQR)
        val btnGuia = findViewById<Button>(R.id.btnGuiaRenal)
        val btnVolver = findViewById<Button>(R.id.btnVolverRolesPaciente)

        // Cargar el último expediente del paciente en Room
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
                        tvEdadSexo.text = "Edad: ${ultimoExpediente?.edad ?: "--"} años | Sexo: ${ultimoExpediente?.sexo ?: "--"}"
                        tvEstadio.text = "Estadio ERC: ${ultimoExpediente?.clasificacionEstadio ?: "En evaluación"}"
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        tvNombre.text = "Sin Expediente Guardado"
                        tvEdadSexo.text = "Realice un tamizaje o ecografía previa"
                        tvEstadio.text = "Bajo Monitoreo General"
                    }
                }
            } catch (e: Exception) {
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

        val datosQR = """
            --- PASAPORTE NEFROSCAN ---
            Paciente: ${exp.nombrePaciente}
            Edad/Sexo: ${exp.edad} / ${exp.sexo}
            Estadio: ${exp.clasificacionEstadio}
            Riesgo Alto: ${if (exp.esRiesgoAlto) "SI" else "NO"}
            eGFR 5y: ${exp.egfr5Anios} mL/min
            eGFR 10y: ${exp.egfr10Anios} mL/min
            Fecha: ${exp.fechaRegistro}
        """.trimIndent()

        val bitmap = crearBitmapQR(datosQR)

        // Usamos la vista de dialog_tarjeta_qr.xml que ya tienes en tu carpeta layout
        val dialogView = layoutInflater.inflate(R.layout.dialog_tarjeta_qr, null)
        val ivQR = dialogView.findViewById<ImageView>(R.id.ivQrCodeDialog)
        val tvInfo = dialogView.findViewById<TextView>(R.id.tvInfoQrDialog)

        ivQR.setImageBitmap(bitmap)
        tvInfo.text = "Paciente: ${exp.nombrePaciente}\nEstadio: ${exp.clasificacionEstadio}"

        AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("Cerrar") { d, _ -> d.dismiss() }
            .show()
    }

    private fun crearBitmapQR(texto: String): Bitmap {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(texto, BarcodeFormat.QR_CODE, 512, 512)
        val bitmap = Bitmap.createBitmap(512, 512, Bitmap.Config.RGB_565)
        for (x in 0 until 512) {
            for (y in 0 until 512) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        return bitmap
    }
}