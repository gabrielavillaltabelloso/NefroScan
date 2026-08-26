package com.insamt.nefroscan

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.animation.LinearInterpolator
import android.widget.Button
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import io.github.sceneview.SceneView
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.math.Scale
import io.github.sceneview.node.ModelNode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var imgPreview: ImageView
    private lateinit var txtStatusTitle: TextView
    private lateinit var lblTitleVisor: TextView
    private lateinit var btnToggleViewMode: Button

    private lateinit var singleSceneView: SceneView
    private var singleModelNode: ModelNode? = null
    private var isHeatmapMode = false

    private lateinit var sbWater: SeekBar
    private lateinit var sbSodium: SeekBar
    private lateinit var sbOpacity: SeekBar
    private lateinit var sbLayers: SeekBar
    private lateinit var lblWater: TextView
    private lateinit var lblSodium: TextView
    private lateinit var lblOpacity: TextView
    private lateinit var lblLayers: TextView
    private lateinit var txtPrediction5Years: TextView
    private lateinit var txtPrediction10Years: TextView

    private var nivelDanoIA: Float = 0.0f
    private var ultimoResultadoIA: Classifier.DiagnosticResult? = null

    private var idPacienteActual: String = "paciente@nefroscan.sv"
    private var nombrePacienteActual: String = "Paciente Comunitario"
    private var edadPacienteActual: Int = 45
    private var idRegistradorActual: String = ""
    private var rolRegistradorActual: String = "MEDICO"
    private var idMedicoAsignadoActual: String? = null

    private val database: NefroScanDatabase by lazy { NefroScanDatabase.getDatabase(applicationContext) }
    private val syncManager: FirebaseSyncManager by lazy { FirebaseSyncManager(applicationContext) }

    private var isPulseActive = false
    private val pulseHandler = Handler(Looper.getMainLooper())
    private var pulseRunnable: Runnable? = null

    private var rotationAnimator: ValueAnimator? = null
    private val localClassifier: Classifier by lazy { Classifier(applicationContext) }

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { selectedUri ->
            val bitmap = uriToBitmap(selectedUri)
            if (bitmap != null) {
                imgPreview.setImageBitmap(bitmap)

                val resultadoCalidad = ImageQualityEvaluator.evaluarCalidadEcografia(bitmap)
                if (!resultadoCalidad.isApta) {
                    AlertDialog.Builder(this)
                        .setTitle("⚠️ Captura No Apta para Diagnóstico")
                        .setMessage(resultadoCalidad.mensajeDiagnostico)
                        .setPositiveButton("Repetir Captura", null)
                        .show()
                    txtStatusTitle.text = "Captura rechazada por baja calidad"
                    return@registerForActivityResult
                }

                txtStatusTitle.text = getString(R.string.status_analyzing)

                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val result = localClassifier.processImage(bitmap)
                        if (result == null) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@MainActivity, "Error al ejecutar el modelo.", Toast.LENGTH_SHORT).show()
                            }
                            return@launch
                        }

                        ultimoResultadoIA = result

                        val porcentajeAfeccion = when (result.pathologyLabel) {
                            "Riñón Normal" -> 0f
                            "Anomalía Física (Litiasis / Quiste)" -> result.anomalyPercentage
                            else -> result.nephropathyPercentage
                        }

                        val severidadTexto = when {
                            porcentajeAfeccion < 15f -> "Normal/Leve"
                            porcentajeAfeccion < 45f -> "Moderada"
                            else -> "Severa/Crítica"
                        }

                        val litrosAgua = withContext(Dispatchers.Main) { sbWater.progress / 10.0f }
                        val sodioProgreso = withContext(Dispatchers.Main) { sbSodium.progress }

                        val textoSodio = when (sodioProgreso) {
                            0 -> getString(R.string.sodium_low)
                            1 -> getString(R.string.sodium_normal)
                            else -> getString(R.string.sodium_high)
                        }

                        var egfrBase = 90
                        if (litrosAgua < 1.0f) egfrBase -= 20
                        if (sodioProgreso == 2) egfrBase -= 15
                        val danoTemp = (porcentajeAfeccion / 100f).coerceIn(0.0f, 1.0f)
                        egfrBase = (egfrBase - (danoTemp * 45f)).toInt().coerceIn(15, 120)

                        val egfr5Anios = (egfrBase * 0.9).toInt().coerceIn(10, 150)
                        val egfr10Anios = (egfrBase * 0.75).toInt().coerceIn(10, 150)

                        val expediente = DiagnosticEntity(
                            idPaciente = idPacienteActual,
                            idRegistrador = idRegistradorActual,
                            rolRegistrador = rolRegistradorActual,
                            idMedicoAsignado = idMedicoAsignadoActual,
                            nombrePaciente = nombrePacienteActual,
                            edadPaciente = edadPacienteActual,
                            porcentajeDano = porcentajeAfeccion.toDouble(),
                            patologiaDetectada = result.pathologyLabel,
                            nivelSeveridad = severidadTexto,
                            litrosAguaDiarios = litrosAgua.toDouble(),
                            nivelSodio = textoSodio.toDoubleOrNull() ?: 1.0,
                            egfrEstimado5Anios = egfr5Anios.toDouble(),
                            egfrEstimado10Anios = egfr10Anios.toDouble(),
                            fechaRegistroTimestamp = System.currentTimeMillis(),
                            sincronizadoConNube = false
                        )

                        database.diagnosticDao().insertarDiagnostico(expediente)

                        withContext(Dispatchers.Main) {
                            txtStatusTitle.text = "Patología: ${result.pathologyLabel} (${"%.1f".format(result.confidence)}%)\n" +
                                    "Afección: ${"%.1f".format(porcentajeAfeccion)}% ($severidadTexto)"

                            nivelDanoIA = danoTemp
                            actualizarGemeloDigital()

                            applyHoloShaderTo3DModel(
                                modelNode = singleModelNode,
                                pathologyLabel = result.pathologyLabel,
                                damagePercentage = porcentajeAfeccion
                            )

                            if (result.pathologyLabel != "Riñón Normal" || porcentajeAfeccion > 2.0f) {
                                start3DScanningAnimation(singleModelNode)
                            } else {
                                stop3DScanningAnimation()
                            }

                            Toast.makeText(this@MainActivity, "Expediente guardado para $nombrePacienteActual", Toast.LENGTH_SHORT).show()
                            intentarSincronizacionFirebase()

                            if (!isFinishing && !isDestroyed) {
                                mostrarPasaporteQR(nombrePacienteActual, result, severidadTexto, porcentajeAfeccion)
                            }
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Log.e("NephroScanAI", "Error crítico en análisis IA", e)
                            Toast.makeText(this@MainActivity, "Error en el análisis: ${e.message}", Toast.LENGTH_LONG).show()
                            txtStatusTitle.text = "Error en el análisis"
                        }
                    }
                }
            } else {
                Toast.makeText(this, "No se pudo cargar la imagen seleccionada", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val prefs = getSharedPreferences("SesionNefroScan", Context.MODE_PRIVATE)
        idRegistradorActual = intent?.getStringExtra("EXTRA_REGISTRADOR_ID")
            ?: prefs.getString("ID_USUARIO", "medico@nefroscan.sv") ?: "medico@nefroscan.sv"
        rolRegistradorActual = intent?.getStringExtra("EXTRA_ROL")
            ?: prefs.getString("ROL_USUARIO", "MEDICO") ?: "MEDICO"

        idPacienteActual = intent?.getStringExtra("EXTRA_PACIENTE_ID")
            ?: intent?.getStringExtra("EXTRA_DUI")
                    ?: "paciente@nefroscan.sv"
        nombrePacienteActual = intent?.getStringExtra("EXTRA_NOMBRE") ?: "Paciente Comunitario"
        edadPacienteActual = intent?.getIntExtra("EXTRA_EDAD", 45) ?: 45
        idMedicoAsignadoActual = intent?.getStringExtra("EXTRA_MEDICO_ASIGNADO") ?: idRegistradorActual

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        imgPreview = findViewById(R.id.imgPreview)
        txtStatusTitle = findViewById(R.id.txtStatusTitle)
        lblTitleVisor = findViewById(R.id.lblTitleVisor)
        btnToggleViewMode = findViewById(R.id.btnToggleViewMode)
        val btnSelect: Button = findViewById(R.id.btnSelect)

        sbWater = findViewById(R.id.sbWater)
        sbSodium = findViewById(R.id.sbSodium)
        sbOpacity = findViewById(R.id.sbOpacity)
        sbLayers = findViewById(R.id.sbLayers)
        lblWater = findViewById(R.id.lblWater)
        lblSodium = findViewById(R.id.lblSodium)
        lblOpacity = findViewById(R.id.lblOpacity)
        lblLayers = findViewById(R.id.lblLayers)
        txtPrediction5Years = findViewById(R.id.txtPrediction5Years)
        txtPrediction10Years = findViewById(R.id.txtPrediction10Years)

        singleSceneView = findViewById(R.id.singleSceneView)

        btnSelect.setOnClickListener {
            galleryLauncher.launch("image/*")
        }

        btnToggleViewMode.setOnClickListener {
            isHeatmapMode = !isHeatmapMode
            if (isHeatmapMode) {
                lblTitleVisor.text = "Gemelo Digital (Mapa de Calor)"
                btnToggleViewMode.text = "Cambiar a Modo: Anatomía 3D"
                actualizarGemeloDigital()
            } else {
                lblTitleVisor.text = getString(R.string.title_anatomy_3d)
                btnToggleViewMode.text = "Cambiar a Modo: Gemelo Digital (Calor)"
                detenerPulsoSimple()
                // Restaurar color base anatómico y escala por defecto
                singleModelNode?.scale = Scale(1.0f, 1.0f, 1.0f)
                singleModelNode?.modelInstance?.materialInstances?.forEach { material ->
                    try {
                        material.setParameter("baseColorFactor", 0.8f, 0.35f, 0.3f, 1.0f)
                        material.setParameter("emissiveFactor", 0.0f, 0.0f, 0.0f)
                    } catch (_: Exception) {}
                }
            }
        }

        configurarControlesSimulacion()

        lblOpacity.text = getString(R.string.lbl_opacity_default, sbOpacity.progress)
        lblLayers.text = getString(R.string.lbl_layers, getString(R.string.layer_all))

        lifecycleScope.launch {
            delay(300)
            inicializarModelo3DUnico()
        }
    }

    private fun mostrarPasaporteQR(nombre: String, resultado: Classifier.DiagnosticResult, severidad: String, porcentajeAfeccion: Float) {
        try {
            val datosQR = """
                --- PASAPORTE NEFROSCAN ---
                ID Paciente: $idPacienteActual
                Paciente: $nombre
                Diagnóstico IA: ${resultado.pathologyLabel}
                Severidad: $severidad
                Confianza: ${"%.1f".format(resultado.confidence)}%
                Nivel Afección: ${"%.1f".format(porcentajeAfeccion)}%
                Registrador: $rolRegistradorActual ($idRegistradorActual)
            """.trimIndent()

            val bitmap = crearBitmapQR(datosQR)
            val dialogView = layoutInflater.inflate(R.layout.dialog_tarjeta_qr, null)
            val ivQR = dialogView.findViewById<ImageView>(R.id.ivQrCodeDialog)
            val tvInfo = dialogView.findViewById<TextView>(R.id.tvInfoQrDialog)

            ivQR?.setImageBitmap(bitmap)
            tvInfo?.text = "Paciente: $nombre\nPatología: ${resultado.pathologyLabel} ($severidad)"

            val dialog = AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton("Cerrar") { d, _ -> d.dismiss() }
                .create()

            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
            dialog.show()
        } catch (e: Exception) {
            Log.e("NephroScanQR", "Error al mostrar pasaporte QR", e)
        }
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

    private fun applyHoloShaderTo3DModel(
        modelNode: ModelNode?,
        pathologyLabel: String,
        damagePercentage: Float
    ) {
        modelNode?.let { node ->
            val isLesionDetected = pathologyLabel != "Riñón Normal" || damagePercentage > 2.0f

            node.modelInstance?.materialInstances?.forEach { material ->
                try {
                    if (isLesionDetected) {
                        material.setParameter("baseColorFactor", 0.1f, 0.4f, 0.9f, 0.45f)
                        val emissiveIntensity = (damagePercentage / 100f).coerceIn(0.3f, 1.0f) * 3.0f
                        material.setParameter("emissiveFactor", 1.0f * emissiveIntensity, 0.0f, 0.1f)
                    } else {
                        val name = material.getName() ?: ""
                        when {
                            name.contains("cortex", true) -> material.setParameter("baseColorFactor", 0.8f, 0.35f, 0.3f, 1.0f)
                            name.contains("medulla", true) -> material.setParameter("baseColorFactor", 0.9f, 0.6f, 0.4f, 1.0f)
                            name.contains("pelvis", true) -> material.setParameter("baseColorFactor", 1.0f, 0.85f, 0.6f, 1.0f)
                            else -> material.setParameter("baseColorFactor", 0.9f, 0.2f, 0.2f, 1.0f)
                        }
                        material.setParameter("emissiveFactor", 0.0f, 0.0f, 0.0f)
                    }
                } catch (e: Exception) {
                    Log.w("NephroScan3D", "Material no soporta el parámetro: ${e.message}")
                }
            }
        }
    }

    private fun start3DScanningAnimation(modelNode: ModelNode?) {
        stop3DScanningAnimation()
        rotationAnimator = ValueAnimator.ofFloat(0f, 360f).apply {
            duration = 8000
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener { animator ->
                val value = animator.animatedValue as Float
                modelNode?.rotation = Rotation(0f, value, 0f)
            }
            start()
        }
    }

    private fun stop3DScanningAnimation() {
        rotationAnimator?.cancel()
        rotationAnimator = null
    }

    fun intentarSincronizacionFirebase() {
        lifecycleScope.launch(Dispatchers.IO) {
            val totalSubidos = syncManager.sincronizarExpedientesPendientes()
            if (totalSubidos > 0) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@MainActivity,
                        "Sincronizados $totalSubidos expediente(s) con Firebase Cloud.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun inicializarModelo3DUnico() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val modelInstance = singleSceneView.modelLoader.loadModelInstance("kidney_model.glb")
                withContext(Dispatchers.Main) {
                    if (modelInstance != null) {
                        val node = ModelNode(
                            modelInstance = modelInstance,
                            scaleToUnits = 1.0f
                        ).apply {
                            position = Position(x = 0.0f, y = 0.0f, z = -1.2f)
                            isRotationEditable = true
                            isScaleEditable = true
                        }
                        singleModelNode = node
                        singleSceneView.addChildNode(node)
                    }
                }
            } catch (e: Exception) {
                Log.e("NephroScan3D", "Error cargando modelo 3D único: ${e.message}")
            }
        }
    }

    private fun configurarControlesSimulacion() {
        val listenerHabitos = object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (isHeatmapMode) actualizarGemeloDigital()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        }

        sbWater.setOnSeekBarChangeListener(listenerHabitos)
        sbSodium.setOnSeekBarChangeListener(listenerHabitos)

        sbLayers.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val label = when {
                    progress < 33 -> getString(R.string.layer_all)
                    progress < 66 -> getString(R.string.layer_medulla)
                    else -> getString(R.string.layer_pelvis)
                }
                lblLayers.text = getString(R.string.lbl_layers, label)
                aplicarCapas(progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        sbOpacity.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                lblOpacity.text = getString(R.string.lbl_opacity_default, progress)
                val alpha = 1.0f - (progress / 100.0f)

                singleModelNode?.modelInstance?.materialInstances?.forEach { material ->
                    try {
                        material.setParameter("baseColorFactor", 0.8f, 0.35f, 0.3f, alpha)
                    } catch (_: Exception) {}
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun aplicarCapas(progress: Int) {
        singleModelNode?.modelInstance?.materialInstances?.forEach { material ->
            try {
                val name = material.getName() ?: ""
                val alpha = when {
                    progress < 33 -> 1.0f
                    progress < 66 -> if (name.contains("cortex", true)) 0.15f else 1.0f
                    else -> {
                        when {
                            name.contains("cortex", true) -> 0.1f
                            name.contains("medulla", true) -> 0.2f
                            else -> 1.0f
                        }
                    }
                }
                when {
                    name.contains("cortex", true) -> material.setParameter("baseColorFactor", 0.8f, 0.35f, 0.3f, alpha)
                    name.contains("medulla", true) -> material.setParameter("baseColorFactor", 0.9f, 0.6f, 0.4f, alpha)
                    name.contains("pelvis", true) -> material.setParameter("baseColorFactor", 1.0f, 0.85f, 0.6f, alpha)
                    else -> material.setParameter("baseColorFactor", 0.9f, 0.2f, 0.2f, alpha)
                }
            } catch (_: Exception) {}
        }
    }

    private fun actualizarGemeloDigital() {
        val litrosAgua = sbWater.progress / 10.0f
        lblWater.text = getString(R.string.lbl_water_intake, litrosAgua)

        val textoSodio = when (sbSodium.progress) {
            0 -> getString(R.string.sodium_low)
            1 -> getString(R.string.sodium_normal)
            else -> getString(R.string.sodium_high)
        }
        lblSodium.text = getString(R.string.lbl_sodium_intake, textoSodio)

        val sodioAlto = sbSodium.progress == 2

        var egfrBase = 90
        if (litrosAgua < 1.0f) egfrBase -= 20
        if (sodioAlto) egfrBase -= 15
        egfrBase = (egfrBase - (nivelDanoIA * 45f)).toInt().coerceIn(15, 120)

        val egfr5Anios = (egfrBase * 0.9).toInt().coerceIn(10, 150)
        val egfr10Anios = (egfrBase * 0.75).toInt().coerceIn(10, 150)

        txtPrediction5Years.text = getString(R.string.prediction_5_years, egfr5Anios)
        txtPrediction10Years.text = getString(R.string.prediction_10_years, egfr10Anios)

        singleModelNode?.let { node ->
            val materials = node.modelInstance?.materialInstances ?: return

            val factorHabitos = ((1.5f - litrosAgua).coerceAtLeast(0f) / 1.5f) + (if (sodioAlto) 0.3f else 0.0f)
            val severidadTotal = (nivelDanoIA + factorHabitos).coerceIn(0.0f, 1.0f)

            // Deformaciones significativamente más prominentes y visibles en la presentación
            val deformacionX = 1.0f - (severidadTotal * 0.60f)
            val deformacionY = 1.0f - (severidadTotal * 0.45f)
            val deformacionZ = 1.0f - (severidadTotal * 0.40f)
            node.scale = Scale(deformacionX, deformacionY, deformacionZ)

            materials.forEach { material ->
                try {
                    val name = material.getName() ?: ""
                    val t = severidadTotal

                    val (r, g, b) = when {
                        name.contains("cortex", true) -> {
                            when {
                                t < 0.3f -> Triple(0.0f, 0.7f, 1.0f)
                                t < 0.6f -> Triple(0.2f, 0.9f, 0.2f)
                                t < 0.85f -> Triple(1.0f, 0.8f, 0.0f)
                                else -> Triple(1.0f, 0.1f, 0.1f)
                            }
                        }
                        name.contains("medulla", true) -> {
                            when {
                                t < 0.4f -> Triple(0.0f, 0.5f, 0.9f)
                                t < 0.7f -> Triple(0.8f, 0.9f, 0.1f)
                                else -> Triple(1.0f, 0.4f, 0.0f)
                            }
                        }
                        else -> Triple(0.0f, 0.3f, 0.8f)
                    }

                    material.setParameter("baseColorFactor", r, g, b, 1.0f)
                    material.setParameter("emissiveFactor", r * 0.5f, g * 0.3f, b * 0.2f)
                } catch (_: Exception) {}
            }

            if (isHeatmapMode && severidadTotal > 0.2f && !isPulseActive) {
                iniciarPulsoSimple(severidadTotal)
            } else if (!isHeatmapMode || severidadTotal <= 0.2f) {
                detenerPulsoSimple()
            }
        }
    }

    private fun iniciarPulsoSimple(severidad: Float) {
        detenerPulsoSimple()
        isPulseActive = true
        val runnable = object : Runnable {
            override fun run() {
                if (!isHeatmapMode) return
                val factor = 0.5f + Math.sin((System.currentTimeMillis() / (200f / severidad)).toDouble()).toFloat() * 0.5f
                singleModelNode?.modelInstance?.materialInstances?.forEach { material ->
                    try {
                        material.setParameter("emissiveFactor", 0.5f * factor, 0.2f * factor, 0.1f * factor)
                    } catch (_: Exception) {}
                }
                pulseHandler.postDelayed(this, (120 / severidad).toLong())
            }
        }
        pulseRunnable = runnable
        pulseHandler.post(runnable)
    }

    private fun detenerPulsoSimple() {
        pulseRunnable?.let { pulseHandler.removeCallbacks(it) }
        pulseRunnable = null
        isPulseActive = false
    }

    private fun uriToBitmap(uri: Uri): Bitmap? {
        return try {
            val inputStream = contentResolver.openInputStream(uri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            originalBitmap?.let { bitmap ->
                val maxWidth = 1024
                val maxHeight = 1024
                var width = bitmap.width
                var height = bitmap.height

                if (width > maxWidth || height > maxHeight) {
                    val ratio = width.toFloat() / height.toFloat()
                    if (ratio > 1) {
                        width = maxWidth
                        height = (maxWidth / ratio).toInt()
                    } else {
                        height = maxHeight
                        width = (maxHeight * ratio).toInt()
                    }
                    Bitmap.createScaledBitmap(bitmap, width, height, true)
                } else {
                    bitmap.copy(Bitmap.Config.ARGB_8888, true)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        detenerPulsoSimple()
        stop3DScanningAnimation()
        try {
            singleModelNode?.let { singleSceneView.removeChildNode(it) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        localClassifier.close()
    }
}