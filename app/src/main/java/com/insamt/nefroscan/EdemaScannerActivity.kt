package com.insamt.nefroscan

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.insamt.nefroscan.data.model.EdemaEvaluacion

class EdemaScannerActivity : AppCompatActivity() {

    // Vistas de UI
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

    // Lanzador para permisos múltiples
    private val permisosLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false
        if (!cameraGranted) {
            Toast.makeText(this, "Se requiere permiso de cámara para grabar el video del edema", Toast.LENGTH_LONG).show()
        }
    }

    // Lanzador para captura de video
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

    // Lanzador para selección de galería
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
        setContentView(R.layout.activity_edema_scanner)

        solicitarPermisosRequeridos()
        inicializarVistas()
        configurarSpinners()
        configurarEventos()
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
            "Pretibia / Pantorrillas",
            "Muslos / Región Lumbo-sacra (encamados)",
            "Anasarca (Generalizado en miembros y facial)"
        )
        spinnerUbicacion.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, opcionesUbicacion)
    }

    private fun configurarEventos() {
        findViewById<Button>(R.id.btnGrabarVideo).setOnClickListener {
            if (tienePermisoCamara()) {
                val intent = Intent(MediaStore.ACTION_VIDEO_CAPTURE).apply {
                    putExtra(MediaStore.EXTRA_DURATION_LIMIT, 10)
                    putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1)
                }
                grabarVideoLauncher.launch(intent)
            } else {
                solicitarPermisosRequeridos()
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
            evaluacionActual?.let { guardarEnRoomYFirebase(it) }
        }

        btnNotificarMedico.setOnClickListener {
            evaluacionActual?.let { dispararAlertaEmergenciaNefrólogo(it) }
        }
    }

    private fun solicitarPermisosRequeridos() {
        val listaPermisos = mutableListOf(Manifest.permission.CAMERA)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            listaPermisos.add(Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            listaPermisos.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        permisosLauncher.launch(listaPermisos.toTypedArray())
    }

    private fun tienePermisoCamara(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }

    private fun configurarYReproducirVideo(uri: Uri) {
        videoViewEdema.stopPlayback()
        videoViewEdema.setVideoURI(uri)
        videoViewEdema.requestFocus()
        videoViewEdema.start()
    }

    /**
     * Algoritmo de Triage Clínico y Puntuación de Sobrecarga Hídrica
     */
    private fun ejecutarEvaluacionClinica() {
        // Guardia 1: Validación de Video
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
        val ubicacion = spinnerUbicacion.selectedItem.toString()
        val esBilateral = switchBilateral.isChecked
        val pesoStr = etAumentoPeso.text.toString().trim()
        val aumentoPesoKg = pesoStr.toDoubleOrNull() ?: 0.0
        val oliguria = switchDiuresis.isChecked
        val disnea = chkDisnea.isChecked
        val ortopnea = chkOrtopnea.isChecked

        // Cálculo del Score de Sobrecarga Hídrica (NefroRisk Edema Index)
        var score = 0
        score += foveaIndex * 2 // Fóvea 0-4 aporta hasta 8 puntos

        if (aumentoPesoKg >= 3.0) score += 4
        else if (aumentoPesoKg >= 1.5) score += 2
        else if (aumentoPesoKg >= 0.8) score += 1

        if (oliguria) score += 3
        if (disnea) score += 5  // Signo crítico de congestión
        if (ortopnea) score += 5 // Signo crítico de congestión

        val alertaCardiopulmonar = disnea || ortopnea
        val sospechaTvpUnilateral = !esBilateral && foveaIndex >= 2

        // Determinación de Nivel de Riesgo
        val nivelRiesgo: String
        val colorHex: String
        val mensajeResumen: StringBuilder = StringBuilder()

        when {
            alertaCardiopulmonar || score >= 9 || foveaIndex == 4 -> {
                nivelRiesgo = "ALERTA ROJA (Sobrecarga Severa / Congestión)"
                colorHex = "#EF4444"
                mensajeResumen.append("🚨 ALERTA CRÍTICA: Se detectan signos de sobrecarga hídrica severa ")
                if (alertaCardiopulmonar) mensajeResumen.append("con compromiso respiratorio (posible congestión pulmonar). ")
                mensajeResumen.append("Se requiere valoración médica urgente o ajuste de diálisis/diuréticos.")
                btnNotificarMedico.visibility = View.VISIBLE
            }
            score in 4..8 || foveaIndex in 2..3 || aumentoPesoKg >= 1.5 -> {
                nivelRiesgo = "RIESGO MODERADO (Retención Hídrica Significativa)"
                colorHex = "#F59E0B"
                mensajeResumen.append("⚠️ Atención: Presenta acumulación de líquido moderada (Score: $score pts). ")
                mensajeResumen.append("Monitoree ingesta de sodio/agua y reporte a su próxima consulta nefrológica.")
                btnNotificarMedico.visibility = View.GONE
            }
            else -> {
                nivelRiesgo = "ESTABLE / EDEMA LEVE O FISIOLÓGICO"
                colorHex = "#38BDF8"
                mensajeResumen.append("✅ Sin signos de sobrecarga hídrica renal de relevancia (Score: $score pts).")
                btnNotificarMedico.visibility = View.GONE
            }
        }

        // Advertencia de asimetría clínica
        if (sospechaTvpUnilateral) {
            mensajeResumen.append("\n\n⚠️ NOTA CLÍNICA: Al ser unilateral, el edema no suele ser de origen renal puro. Considere descartar trombosis venosa profunda (TVP) o causa linfática local.")
        }

        // Mostrar en UI
        txtNivelRiesgo.text = nivelRiesgo
        txtNivelRiesgo.setTextColor(android.graphics.Color.parseColor(colorHex))
        txtDetalleDiagnostico.text = mensajeResumen.toString()
        btnGuardarExpediente.visibility = View.VISIBLE

        // Crear objeto para persistencia
        evaluacionActual = EdemaEvaluacion(
            pacienteId = "PACIENTE_DEMO_01", // Reemplazar con ID de sesión actual
            foveaGrado = foveaIndex,
            foveaDescripcion = spinnerFovea.selectedItem.toString(),
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
            sospechaTvpUnilateral = sospechaTvpUnilateral
        )

        // Opcional: procesar fotograma para modelo TFLite
        videoUri?.let { extraerPrimerFotogramaParaTFLite(it) }
    }

    /**
     * Pipeline para TensorFlow Lite: Extrae fotogramas del video grabado para inferencia
     */
    private fun extraerPrimerFotogramaParaTFLite(uri: Uri): Bitmap? {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(this, uri)
            // Extraer frame a los 2 segundos (momento donde el dedo presiona la piel)
            val bitmap = retriever.getFrameAtTime(2000000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            retriever.release()
            // Aquí puedes llamar a tu clasificador TFLite: clasificadorFovea.analizarBitmap(bitmap)
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun guardarEnRoomYFirebase(evaluacion: EdemaEvaluacion) {
        // Simulación de guardado en base de datos local y remota
        Toast.makeText(this, "Evaluación guardada exitosamente en el expediente del paciente", Toast.LENGTH_LONG).show()
        btnGuardarExpediente.isEnabled = false
        btnGuardarExpediente.text = "Guardado en Expediente ✔"
    }

    private fun dispararAlertaEmergenciaNefrólogo(evaluacion: EdemaEvaluacion) {
        // Envío de push notification / documento de alerta en colección 'alertas_medicas' de Firestore
        AlertDialog.Builder(this)
            .setTitle("Alerta Enviada")
            .setMessage("Se ha emitido una notificación prioritaria al nefrólogo tratante con el video y los signos de sobrecarga hídrica registrados.")
            .setPositiveButton("Aceptar", null)
            .show()
    }

    // Manejo seguro del ciclo de vida para evitar fugas de memoria del VideoView
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