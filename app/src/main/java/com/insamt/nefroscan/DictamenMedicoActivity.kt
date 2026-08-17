package com.insamt.nefroscan

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DictamenMedicoActivity : AppCompatActivity() {

    private lateinit var tvPacienteDictamen: TextView
    private lateinit var tvEstadioKdigoDictamen: TextView
    private lateinit var tvCuerpoNotaSoap: TextView

    private val database: NefroScanDatabase by lazy { NefroScanDatabase.getDatabase(applicationContext) }
    private var idMedicoSesion: String = ""
    private var nombreMedicoSesion: String = ""
    private var textoNotaGenerada: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dictamen_medico)

        // 1. Obtener la sesión activa del médico
        val prefs = getSharedPreferences("SesionNefroScan", Context.MODE_PRIVATE)
        idMedicoSesion = prefs.getString("ID_USUARIO", "") ?: ""
        nombreMedicoSesion = prefs.getString("NOMBRE_USUARIO", "Dr. Especialista") ?: "Dr. Especialista"

        tvPacienteDictamen = findViewById(R.id.tvPacienteDictamen)
        tvEstadioKdigoDictamen = findViewById(R.id.tvEstadioKdigoDictamen)
        tvCuerpoNotaSoap = findViewById(R.id.tvCuerpoNotaSoap)

        findViewById<MaterialButton>(R.id.btnCerrarDictamen).setOnClickListener { finish() }

        // 2. Acción: Copiar al portapapeles
        findViewById<MaterialButton>(R.id.btnCopiarNota).setOnClickListener {
            if (textoNotaGenerada.isNotEmpty()) {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Nota SOAP NefroScan", textoNotaGenerada)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this, "Nota clínica copiada al portapapeles.", Toast.LENGTH_SHORT).show()
            }
        }

        // 3. Acción: Compartir dictamen clínico
        findViewById<MaterialButton>(R.id.btnCompartirDictamen).setOnClickListener {
            if (textoNotaGenerada.isNotEmpty()) {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "Dictamen Clínico NefroScan AI")
                    putExtra(Intent.EXTRA_TEXT, textoNotaGenerada)
                }
                startActivity(Intent.createChooser(shareIntent, "Compartir Dictamen Médico"))
            }
        }

        // 4. Generar y renderizar la nota SOAP desde la base de datos
        generarDictamenClinico()
    }

    private fun generarDictamenClinico() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val expedientes = database.diagnosticDao().obtenerListaPorMedico(idMedicoSesion)

                withContext(Dispatchers.Main) {
                    if (expedientes.isNotEmpty()) {
                        val exp = expedientes.first() // Último diagnóstico evaluado
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
                            SISTEMA NEFROSCAN • NOTA DE EVOLUCIÓN CLÍNICA
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
}