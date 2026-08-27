package com.insamt.nefroscan

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
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
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class DictamenMedicoActivity : AppCompatActivity() {

    private lateinit var tvPacienteDictamen: TextView
    private lateinit var tvEstadioKdigoDictamen: TextView
    private lateinit var tvCuerpoNotaSoap: TextView

    private val database: NefroScanDatabase by lazy { NefroScanDatabase.getDatabase(applicationContext) }
    private var idMedicoSesion: String = ""
    private var nombreMedicoSesion: String = ""
    private var textoNotaGenerada: String = ""
    private var ultimoDiagnostico: DiagnosticEntity? = null

    companion object {
        private const val REQUEST_WRITE_PERMISSION = 102
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dictamen_medico)

        // Obtener sesión del médico
        val prefs = getSharedPreferences("SesionNefroScan", Context.MODE_PRIVATE)
        idMedicoSesion = prefs.getString("ID_USUARIO", "") ?: ""
        nombreMedicoSesion = prefs.getString("NOMBRE_USUARIO", "Dr. Especialista") ?: "Dr. Especialista"

        tvPacienteDictamen = findViewById(R.id.tvPacienteDictamen)
        tvEstadioKdigoDictamen = findViewById(R.id.tvEstadioKdigoDictamen)
        tvCuerpoNotaSoap = findViewById(R.id.tvCuerpoNotaSoap)

        // Configurar toolbar con botón de cierre
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbarDictamen)
        toolbar.setNavigationOnClickListener { finish() }

        // Acción: Copiar nota al portapapeles
        findViewById<MaterialButton>(R.id.btnCopiarNota).setOnClickListener {
            if (textoNotaGenerada.isNotEmpty()) {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Nota SOAP NefroScan", textoNotaGenerada)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this, "Nota clínica copiada al portapapeles.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "No hay nota generada para copiar.", Toast.LENGTH_SHORT).show()
            }
        }

        // Acción: Compartir / PDF
        findViewById<MaterialButton>(R.id.btnCompartirDictamen).setOnClickListener {
            if (textoNotaGenerada.isNotEmpty()) {
                // Mostrar opciones: compartir texto o exportar PDF
                AlertDialog.Builder(this)
                    .setTitle("Dictamen Clínico")
                    .setMessage("¿Qué deseas hacer con el dictamen?")
                    .setPositiveButton("Exportar PDF") { _, _ ->
                        exportarDictamenPdf()
                    }
                    .setNegativeButton("Compartir texto") { _, _ ->
                        compartirTexto()
                    }
                    .setNeutralButton("Cancelar", null)
                    .show()
            }
        }

        // Generar la nota SOAP desde la base de datos
        generarDictamenClinico()
    }

    private fun generarDictamenClinico() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val expedientes = database.diagnosticDao().obtenerListaPorMedico(idMedicoSesion)

                withContext(Dispatchers.Main) {
                    if (expedientes.isNotEmpty()) {
                        val exp = expedientes.first()
                        ultimoDiagnostico = exp
                        val fecha = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                            .format(Date(exp.fechaRegistroTimestamp))

                        val estadioKDIGO = when {
                            exp.egfrEstimado5Anios >= 90 -> "Estadio G1 (Filtrado Normal/Alto)"
                            exp.egfrEstimado5Anios >= 60 -> "Estadio G2 (Levemente Disminuido)"
                            exp.egfrEstimado5Anios >= 45 -> "Estadio G3a (Disminución Ligera a Moderada)"
                            exp.egfrEstimado5Anios >= 30 -> "Estadio G3b (Disminución Moderada a Grave)"
                            exp.egfrEstimado5Anios >= 15 -> "Estadio G4 (Gravemente Disminuido)"
                            else -> "Estadio G5 (Falla Renal Terminal)"
                        }

                        tvPacienteDictamen.text = "Paciente: ${exp.nombrePaciente} (${exp.edadPaciente} años)"
                        tvEstadioKdigoDictamen.text = "Clasificación: $estadioKDIGO"

                        textoNotaGenerada = """
                            SISTEMA NEFROSCAN – NOTA DE EVOLUCIÓN CLÍNICA
                            Médico Tratante: $nombreMedicoSesion ($idMedicoSesion)
                            Fecha y Hora: $fecha
                            ---------------------------------------------------
                            
                            [S] SUBJETIVO:
                            Paciente evaluado bajo monitoreo nefrológico. Refiere ingesta hídrica diaria de ${exp.litrosAguaDiarios} L/día. Nivel de ingesta de sodio: ${if (exp.nivelSodio > 1.5) "Elevado" else "Controlado"}.
                            
                            [O] OBJETIVO:
                            • Hallazgo Ecográfico e IA: ${exp.patologiaDetectada}
                            • Porcentaje de Daño Parenquimatoso: ${exp.porcentajeDano}%
                            • Nivel de Severidad Tisular: ${exp.nivelSeveridad}
                            • Tasa eGFR Proyectada a 5 Años: ${exp.egfrEstimado5Anios} mL/min/1.73m²
                            • Tasa eGFR Proyectada a 10 Años: ${exp.egfrEstimado10Anios} mL/min/1.73m²
                            
                            [A] ANÁLISIS:
                            Paciente clasificado en $estadioKDIGO. Riesgo de progresión acelerada ${if (exp.porcentajeDano >= 30.0) "ALTO debido a compromiso cortical y tubular ecográfico." else "MODERADO/BAJO bajo manejo conservador y nefroprotección."}
                            
                            [P] PLAN Y PRESCRIPCIÓN:
                            1. Hidratación: Meta terapéutica ajustada a ${if (exp.litrosAguaDiarios < 2.0) "2.5 L/día fraccionados" else "${exp.litrosAguaDiarios} L/día"}.
                            2. Farmacovigilancia: Contraindicación estricta de AINEs (Ibuprofeno/Diclofenaco/Ketorolaco). Monitorizar uso de IECA/ARA-II.
                            3. Seguimiento: Control de creatinina sérica y reevaluación ecográfica en ${if (exp.egfrEstimado5Anios < 45.0) "30 días" else "90 días"}.
                        """.trimIndent()

                        tvCuerpoNotaSoap.text = textoNotaGenerada
                    } else {
                        tvPacienteDictamen.text = "Sin expedientes registrados"
                        tvEstadioKdigoDictamen.text = "No se encontraron datos clínicos"
                        tvCuerpoNotaSoap.text = "Realice una evaluación ecográfica previa desde el menú principal para generar el dictamen."
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    tvPacienteDictamen.text = "Error de sincronización"
                    tvCuerpoNotaSoap.text = "No fue posible acceder a la base de datos local."
                }
            }
        }
    }

    private fun compartirTexto() {
        if (textoNotaGenerada.isNotEmpty()) {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Dictamen Clínico NefroScan AI")
                putExtra(Intent.EXTRA_TEXT, textoNotaGenerada)
            }
            startActivity(Intent.createChooser(shareIntent, "Compartir Dictamen Médico"))
        }
    }

    // ===================== EXPORTAR PDF =====================
    private fun exportarDictamenPdf() {
        val diagnostico = ultimoDiagnostico ?: run {
            Toast.makeText(this, "No hay diagnóstico para exportar.", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val pdfFile = crearPdfDictamen(diagnostico)
            withContext(Dispatchers.Main) {
                if (pdfFile != null) {
                    AlertDialog.Builder(this@DictamenMedicoActivity)
                        .setTitle("PDF generado")
                        .setMessage("Se guardó correctamente en:\n${pdfFile.absolutePath}")
                        .setPositiveButton("Abrir PDF") { _, _ -> abrirPdf(pdfFile) }
                        .setNegativeButton("Cerrar", null)
                        .show()
                } else {
                    Toast.makeText(this@DictamenMedicoActivity, "Error al generar el PDF.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun crearPdfDictamen(diagnostico: DiagnosticEntity): File? {
        // Solicitar permiso si es necesario (Android 9 y anteriores)
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
        canvas.drawText("Sistema NefroScan AI - Dictamen Clínico", pageWidth / 2f, 70f, paint)

        paint.color = Color.parseColor("#00B4D8")
        canvas.drawRect(0f, 120f, pageWidth.toFloat(), 125f, paint)

        // ============ DATOS DEL PACIENTE Y MÉDICO ============
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
        val fechaTexto = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(diagnostico.fechaRegistroTimestamp))
        canvas.drawText("Fecha de Evaluación: $fechaTexto", leftMargin, y, paint)
        y += 30

        // ============ NOTA SOAP ============
        paint.color = Color.parseColor("#03045E")
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("NOTA DE EVOLUCIÓN CLÍNICA (SOAP)", leftMargin, y, paint)
        y += 25

        paint.color = Color.BLACK
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 12f

        // Dividir el texto en líneas para que quepa en la página
        val lineas = textoNotaGenerada.split("\n")
        for (linea in lineas) {
            // Si excede el ancho de página, se trunca (puedes mejorar con texto ajustado)
            canvas.drawText(linea, leftMargin, y, paint)
            y += 18
            if (y > pageHeight - 100) {
                // Salto de página si es necesario
                pdfDocument.finishPage(page)
                val newPage = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 2).create()
                val newPageCanvas = pdfDocument.startPage(newPage).canvas
                newPageCanvas.drawColor(Color.WHITE)
                y = 100f
                paint.textAlign = Paint.Align.LEFT
                // Continuar dibujando en la nueva página
                // En este ejemplo simple, si se pasa, se corta (no se implementa paginación avanzada)
                break
            }
        }

        // Pie de página
        paint.color = Color.parseColor("#64748B")
        paint.textSize = 10f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("Generado automáticamente por NefroScan AI - ${fechaTexto}", pageWidth / 2f, pageHeight - 30f, paint)

        pdfDocument.finishPage(page)

        val fileName = "Dictamen_${diagnostico.nombrePaciente.replace(" ", "_")}_${System.currentTimeMillis()}.pdf"

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
                // Devolver un File temporal para abrir
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