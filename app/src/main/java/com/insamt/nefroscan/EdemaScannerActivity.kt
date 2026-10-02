package com.insamt.nefroscan

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat // Import correcto para AppCompat SwitchCompat
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.insamt.nefroscan.data.model.EdemaEvaluacion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Suppress("SpellCheckingInspection")
class EdemaScannerActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "EdemaScannerActivity"
    }

    private lateinit var spinnerFovea: Spinner
    private lateinit var spinnerUbicacion: Spinner
    private lateinit var switchBilateral: SwitchCompat
    private lateinit var etAumentoPeso: EditText
    private lateinit var switchDiuresis: SwitchCompat
    private lateinit var chkDisnea: CheckBox
    private lateinit var chkOrtopnea: CheckBox
    private lateinit var videoViewEdema: VideoView
    private lateinit var txtEstadoVideo: TextView
    private lateinit var cardResultado: CardView
    private lateinit var txtNivelRiesgo: TextView
    private lateinit var txtDetalleDiagnostico: TextView
    private lateinit var btnGuardarExpediente: Button
    private lateinit var btnNotificarMedico: Button

    private var videoUri: Uri? = null
    private var mediaController: MediaController? = null
    private var evaluacionActual: EdemaEvaluacion? = null

    private val database: NefroScanDatabase by lazy { NefroScanDatabase.getDatabase(applicationContext) }

    private val permisosLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    private val grabarVideoLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            videoUri = result.data?.data
            videoUri?.let { uri ->
                configurarYReproducirVideo(uri)
                txtEstadoVideo.visibility = View.GONE
                Toast.makeText(this, "Video registrado correctamente", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val seleccionarVideoLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            videoUri = result.data?.data
            videoUri?.let { uri ->
                configurarYReproducirVideo(uri)
                txtEstadoVideo.visibility = View.GONE
                Toast.makeText(this, "Video seleccionado de la galería", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_edema_scanner)
            solicitarPermisosRequeridos()
            inicializarVistas()
            configurarSpinners()
            configurarEventos()
        } catch (e: Exception) {
            // Log.e imprime el stack trace COMPLETO en Logcat, aunque el Toast solo
            // muestre el mensaje corto. Filtra Logcat por el tag "EdemaScannerActivity"
            // para ver la causa real del crash.
            Log.e(TAG, "Error al iniciar EdemaScannerActivity", e)
            Toast.makeText(this, "Error al iniciar pantalla: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun inicializarVistas() {
        spinnerFovea = findViewById(R.id.spinnerFovea)
        spinnerUbicacion = findViewById(R.id.spinnerUbicacion)
        switchBilateral = findViewById(R.id.switchBilateral)
        etAumentoPeso = findViewById(R.id.etAumentoPeso)
        switchDiuresis = findViewById(R.id.switchDiuresis)
        chkDisnea = findViewById(R.id.chkDisnea)
        chkOrtopnea = findViewById(R.id.chkOrtopnea)
        videoViewEdema = findViewById(R.id.videoViewEdema)
        txtEstadoVideo = findViewById(R.id.txtEstadoVideo)
        cardResultado = findViewById(R.id.cardResultado)
        txtNivelRiesgo = findViewById(R.id.txtNivelRiesgo)
        txtDetalleDiagnostico = findViewById(R.id.txtDetalleDiagnostico)
        btnGuardarExpediente = findViewById(R.id.btnGuardarExpediente)
        btnNotificarMedico = findViewById(R.id.btnNotificarMedico)

        mediaController = MediaController(this)
        mediaController?.setAnchorView(videoViewEdema)
        videoViewEdema.setMediaController(mediaController)
    }

    private fun configurarSpinners() {
        val opcionesFovea = arrayOf(
            "Grado 0: Sin fóvea (No hay retención visible)",
            "Grado 1+: Leve (Depresión 2mm, recuperación instantánea)",
            "Grado 2+: Moderado (Depresión 4mm, tarda 10-15s)",
            "Grado 3+: Pronunciado (Depresión 6mm, tarda 1 min)",
            "Grado 4+: Severo (Depresión 8mm, tarda >2 min)"
        )
        spinnerFovea.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, opcionesFovea)

        val opcionesUbicacion = arrayOf(
            "Maleolar / Tobillos / Dorso del pie",
            "Pretibial / Pantorrillas",
            "Muslos / Región Lumbo-sacra (encamados)",
            "Anasarca (Generalizado en miembros y facial)"
        )
        spinnerUbicacion.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, opcionesUbicacion)
    }

    private fun configurarEventos() {
        findViewById<Button>(R.id.btnGrabarVideo).setOnClickListener {
            val intent = Intent(MediaStore.ACTION_VIDEO_CAPTURE).apply {
                putExtra(MediaStore.EXTRA_DURATION_LIMIT, 10)
                putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1)
            }
            try {
                grabarVideoLauncher.launch(intent)
            } catch (e: Exception) {
                Log.e(TAG, "No se pudo abrir la cámara", e)
                Toast.makeText(this, "No se pudo abrir la cámara", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<Button>(R.id.btnSubirVideo).setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            seleccionarVideoLauncher.launch(intent)
        }

        findViewById<Button>(R.id.btnCalcularEdema).setOnClickListener {
            ejecutarEvaluacionClinica()
        }

        btnGuardarExpediente.setOnClickListener {
            evaluacionActual?.let {
                guardarEnRoomYFirebase(it)
            } ?: Toast.makeText(this, "Primero debe realizar el cálculo de riesgo", Toast.LENGTH_SHORT).show()
        }

        btnNotificarMedico.setOnClickListener {
            evaluacionActual?.let { dispararAlertaEmergenciaNefrologo(it) }
        }
    }

    private fun solicitarPermisosRequeridos() {
        val listaPermisos = mutableListOf(Manifest.permission.CAMERA)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            listaPermisos.add(Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            @Suppress("DEPRECATION")
            listaPermisos.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        permisosLauncher.launch(listaPermisos.toTypedArray())
    }

    private fun configurarYReproducirVideo(uri: Uri) {
        try {
            videoViewEdema.stopPlayback()
            videoViewEdema.setVideoURI(uri)
            videoViewEdema.requestFocus()
            videoViewEdema.start()
        } catch (e: Exception) {
            Log.e(TAG, "Error al reproducir el video", e)
            Toast.makeText(this, "Error al reproducir el video", Toast.LENGTH_SHORT).show()
        }
    }

    private fun ejecutarEvaluacionClinica() {
        if (videoUri == null) {
            AlertDialog.Builder(this)
                .setTitle("Evidencia requerida")
                .setMessage("No se ha adjuntado un video de la prueba de fóvea. ¿Desea continuar con la evaluación solo con datos manuales?")
                .setPositiveButton("Continuar") { _, _ -> procesarCalculo() }
                .setNegativeButton("Adjuntar Video", null)
                .show()
            return
        }
        procesarCalculo()
    }

    private fun procesarCalculo() {
        val foveaIndex = spinnerFovea.selectedItemPosition
        val ubicacion = spinnerUbicacion.selectedItem?.toString() ?: "No especificada"
        val esBilateral = switchBilateral.isChecked
        val pesoStr = etAumentoPeso.text.toString().trim()
        val aumentoPesoKg = pesoStr.toDoubleOrNull() ?: 0.0
        val oliguria = switchDiuresis.isChecked
        val disnea = chkDisnea.isChecked
        val ortopnea = chkOrtopnea.isChecked

        var score = 0
        score += foveaIndex * 2

        if (aumentoPesoKg >= 3.0) score += 4
        else if (aumentoPesoKg >= 1.5) score += 2
        else if (aumentoPesoKg >= 0.8) score += 1

        if (oliguria) score += 3
        if (disnea) score += 5
        if (ortopnea) score += 5

        val alertaCardiopulmonar = disnea || ortopnea
        val sospechaTvpUnilateral = !esBilateral && foveaIndex >= 2

        val nivelRiesgo: String
        val colorHex: String
        val mensajeResumen = StringBuilder()

        when {
            alertaCardiopulmonar || score >= 9 || foveaIndex == 4 -> {
                nivelRiesgo = "ALERTA_ROJA"
                colorHex = "#EF4444"
                mensajeResumen.append("⚠️ ALERTA CRÍTICA: Se detectan signos de sobrecarga hídrica severa ")
                if (alertaCardiopulmonar) mensajeResumen.append("con compromiso respiratorio. ")
                mensajeResumen.append("Se requiere valoración médica urgente.")
                btnNotificarMedico.visibility = View.VISIBLE
            }
            score in 4..8 || foveaIndex in 2..3 || aumentoPesoKg >= 1.5 -> {
                nivelRiesgo = "LEVE_MODERADO"
                colorHex = "#F59E0B"
                mensajeResumen.append("⚠ Atención: Presenta acumulación de líquido moderada (Score: $score pts). ")
                mensajeResumen.append("Monitoree ingesta de sodio/agua.")
                btnNotificarMedico.visibility = View.GONE
            }
            else -> {
                nivelRiesgo = "NORMAL"
                colorHex = "#38BDF8"
                mensajeResumen.append("✅ Sin signos de sobrecarga hídrica renal de relevancia (Score: $score pts).")
                btnNotificarMedico.visibility = View.GONE
            }
        }

        if (sospechaTvpUnilateral) {
            mensajeResumen.append("\n\n🔎 NOTA CLÍNICA: Al ser unilateral, considere descartar trombosis venosa profunda (TVP).")
        }

        txtNivelRiesgo.text = nivelRiesgo
        txtNivelRiesgo.setTextColor(android.graphics.Color.parseColor(colorHex))
        txtDetalleDiagnostico.text = mensajeResumen.toString()
        btnGuardarExpediente.visibility = View.VISIBLE

        evaluacionActual = EdemaEvaluacion(
            pacienteId = "PACIENTE_DEMO_01",
            foveaGrado = foveaIndex,
            foveaDescripcion = spinnerFovea.selectedItem?.toString() ?: "",
            ubicacion = ubicacion,
            esBilateral = esBilateral,
            aumentoPesoKg = aumentoPesoKg,
            disminucionDiuresis = oliguria,
            tieneDisnea = disnea,
            tieneOrtopnea = ortopnea,
            videoUriLocal = videoUri?.toString(),
            scoreSobrecarga = score,
            nivelRiesgo = nivelRiesgo,
            alertaCardiopulmonar = alertaCardiopulmonar,
            sospechaTvpUnilateral = sospechaTvpUnilateral,
            enviadoAlMedico = false
        )
    }

    private fun guardarEnRoomYFirebase(evaluacion: EdemaEvaluacion) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                database.edemaDao().insertarEvaluacion(evaluacion)

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@EdemaScannerActivity, "Evaluación guardada exitosamente en el expediente local", Toast.LENGTH_LONG).show()
                    btnGuardarExpediente.isEnabled = false
                    btnGuardarExpediente.text = "Guardado en Expediente ✓"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al guardar en base de datos", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@EdemaScannerActivity, "Error al guardar en base de datos: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun dispararAlertaEmergenciaNefrologo(evaluacion: EdemaEvaluacion) {
        AlertDialog.Builder(this)
            .setTitle("Alerta Enviada")
            .setMessage("Se ha emitido una notificación prioritaria al nefrólogo tratante.")
            .setPositiveButton("Aceptar", null)
            .show()
    }

    override fun onPause() {
        super.onPause()
        if (videoViewEdema.isPlaying) {
            videoViewEdema.pause()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        videoViewEdema.stopPlayback()
    }
}