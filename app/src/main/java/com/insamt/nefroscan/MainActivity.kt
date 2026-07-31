package com.insamt.nefroscan

import android.animation.ValueAnimator
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.math.Scale
import io.github.sceneview.node.ModelNode
import io.github.sceneview.SceneView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var imgPreview: ImageView
    private lateinit var txtStatusTitle: TextView

    private lateinit var anatomicalSceneView: SceneView
    private lateinit var heatmapSceneView: SceneView

    private var anatomicalNode: ModelNode? = null
    private var heatmapNode: ModelNode? = null

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

    private var nombrePacienteActual: String = "Paciente Anónimo"
    private var edadPacienteActual: Int = 0

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
                txtStatusTitle.text = getString(R.string.status_analyzing)

                // BLINDAJE: Ejecución totalmente aislada en IO para evitar congelar la UI
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val result = localClassifier.processImage(bitmap)
                        ultimoResultadoIA = result

                        val severidadTexto = when {
                            result.damagePercentage < 10f -> "Leve"
                            result.damagePercentage < 30f -> "Moderada"
                            else -> "Severa"
                        }

                        // Guardado seguro en base de datos en segundo plano
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
                        val danoTemp = (result.damagePercentage / 100f).coerceIn(0.0f, 1.0f)
                        egfrBase = (egfrBase - (danoTemp * 45f)).toInt().coerceIn(15, 120)

                        val egfr5Anios = (egfrBase * 0.9).toInt().coerceIn(10, 150)
                        val egfr10Anios = (egfrBase * 0.75).toInt().coerceIn(10, 150)

                        val expediente = DiagnosticEntity(
                            nombrePaciente = nombrePacienteActual,
                            edadPaciente = edadPacienteActual,
                            porcentajeDano = result.damagePercentage,
                            patologiaDetectada = result.pathologyLabel,
                            nivelSeveridad = severidadTexto,
                            litrosAguaDiarios = litrosAgua,
                            nivelSodio = textoSodio,
                            egfrEstimado5Anios = egfr5Anios,
                            egfrEstimado10Anios = egfr10Anios
                        )

                        database.diagnosticDao().insertarDiagnostico(expediente)

                        // Retorno seguro al Hilo Principal para actualizar elementos visuales
                        withContext(Dispatchers.Main) {
                            txtStatusTitle.text = "Patología: ${result.pathologyLabel} (${"%.1f".format(result.confidence * 100)}%)\n" +
                                    "Área Afectada: ${"%.1f".format(result.damagePercentage)}% ($severidadTexto)"

                            nivelDanoIA = danoTemp
                            actualizarGemeloDigital()

                            applyHoloShaderTo3DModel(
                                modelNode = anatomicalNode,
                                pathologyLabel = result.pathologyLabel,
                                damagePercentage = result.damagePercentage
                            )

                            if (result.pathologyLabel != "Normal" || result.damagePercentage > 2.0f) {
                                start3DScanningAnimation(anatomicalNode)
                            } else {
                                stop3DScanningAnimation()
                            }

                            Toast.makeText(this@MainActivity, "Expediente guardado para $nombrePacienteActual", Toast.LENGTH_SHORT).show()
                            intentarSincronizacionFirebase()

                            if (!isFinishing && !isDestroyed) {
                                mostrarPasaporteQR(nombrePacienteActual, result, severidadTexto)
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

        nombrePacienteActual = intent?.getStringExtra("EXTRA_NOMBRE") ?: "Paciente Anónimo"
        edadPacienteActual = intent?.getIntExtra("EXTRA_EDAD", 0) ?: 0

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        imgPreview = findViewById(R.id.imgPreview)
        txtStatusTitle = findViewById(R.id.txtStatusTitle)
        val btnSelect: Button = findViewById(R.id.btnSelect)
        val btnProyectarAR: Button = findViewById(R.id.btnProyectarAR)

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

        anatomicalSceneView = findViewById(R.id.anatomicalSceneView)
        heatmapSceneView = findViewById(R.id.heatmapSceneView)

        btnSelect.setOnClickListener {
            galleryLauncher.launch("image/*")
        }

        btnProyectarAR.setOnClickListener {
            Toast.makeText(this, "Modo AR Activo: Apunte a una superficie plana.", Toast.LENGTH_LONG).show()
            anatomicalNode?.let { node ->
                node.isPositionEditable = true
                node.isScaleEditable = true
                Toast.makeText(this, "¡Riñón holográfico proyectado en el espacio real!", Toast.LENGTH_SHORT).show()
            }
        }

        configurarControlesSimulacion()

        lblOpacity.text = getString(R.string.lbl_opacity_default, sbOpacity.progress)
        lblLayers.text = getString(R.string.lbl_layers, getString(R.string.layer_all))

        anatomicalSceneView.post {
            heatmapSceneView.post {
                inicializarModelos3D()
            }
        }
    }

    private fun mostrarPasaporteQR(nombre: String, resultado: Classifier.DiagnosticResult, severidad: String) {
        try {
            val dialogView = layoutInflater.inflate(R.layout.dialog_tarjeta_qr, null)
            val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
                .setView(dialogView)
                .create()

            val tvNombre = dialogView.findViewById<TextView>(R.id.tvCardNombre)
            val tvDiagnostico = dialogView.findViewById<TextView>(R.id.tvCardDiagnostico)
            val btnCerrar = dialogView.findViewById<Button>(R.id.btnCerrarCard)

            tvNombre?.text = "Paciente: $nombre"
            tvDiagnostico?.text = "Patología: ${resultado.pathologyLabel} ($severidad)"

            btnCerrar?.setOnClickListener {
                dialog.dismiss()
            }

            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
            dialog.show()
        } catch (e: Exception) {
            Log.e("NephroScanQR", "Error al mostrar pasaporte QR", e)
        }
    }

    private fun applyHoloShaderTo3DModel(
        modelNode: ModelNode?,
        pathologyLabel: String,
        damagePercentage: Float
    ) {
        modelNode?.let { node ->
            val isLesionDetected = pathologyLabel != "Normal" || damagePercentage > 2.0f

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

    private fun inicializarModelos3D() {
        try {
            val nodeAnat = ModelNode(anatomicalSceneView.engine)
            lifecycleScope.launch {
                try {
                    nodeAnat.loadModelGlb(
                        context = this@MainActivity,
                        glbFileLocation = "kidney_model.glb",
                        autoAnimate = true,
                        scaleToUnits = 1.0f
                    )
                    nodeAnat.position = Position(x = 0.0f, y = 0.0f, z = -1.2f)
                    nodeAnat.isRotationEditable = true
                    nodeAnat.isScaleEditable = true

                    nodeAnat.modelInstance?.materialInstances?.forEach { material ->
                        try {
                            val name = material.getName() ?: ""
                            when {
                                name.contains("cortex", true) -> {
                                    material.setParameter("baseColorFactor", 0.8f, 0.35f, 0.3f, 1.0f)
                                    material.setParameter("emissiveFactor", 0.2f, 0.1f, 0.05f)
                                }
                                name.contains("medulla", true) -> {
                                    material.setParameter("baseColorFactor", 0.9f, 0.6f, 0.4f, 1.0f)
                                    material.setParameter("emissiveFactor", 0.1f, 0.05f, 0.0f)
                                }
                                name.contains("pelvis", true) -> {
                                    material.setParameter("baseColorFactor", 1.0f, 0.85f, 0.6f, 1.0f)
                                    material.setParameter("emissiveFactor", 0.0f, 0.0f, 0.0f)
                                }
                                else -> {
                                    material.setParameter("baseColorFactor", 0.9f, 0.2f, 0.2f, 1.0f)
                                    material.setParameter("emissiveFactor", 0.3f, 0.1f, 0.1f)
                                }
                            }
                        } catch (e: Exception) { }
                    }

                    anatomicalNode = nodeAnat
                    anatomicalSceneView.addChild(nodeAnat)
                } catch (e: Exception) {
                    Log.e("NephroScan3D", "Error cargando modelo anatómico: ${e.message}")
                }
            }
        } catch (e: Exception) { e.printStackTrace() }

        try {
            val nodeHeat = ModelNode(heatmapSceneView.engine)
            lifecycleScope.launch {
                try {
                    nodeHeat.loadModelGlb(
                        context = this@MainActivity,
                        glbFileLocation = "kidney_model.glb",
                        autoAnimate = true,
                        scaleToUnits = 1.0f
                    )
                    nodeHeat.position = Position(x = 0.0f, y = 0.0f, z = -1.2f)
                    nodeHeat.isRotationEditable = true
                    nodeHeat.isScaleEditable = true

                    heatmapNode = nodeHeat
                    heatmapSceneView.addChild(nodeHeat)

                    nodeHeat.modelInstance?.materialInstances?.let {
                        actualizarGemeloDigital()
                    }
                } catch (e: Exception) {
                    Log.e("NephroScan3D", "Error cargando modelo de calor: ${e.message}")
                }
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun configurarControlesSimulacion() {
        val listenerHabitos = object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                actualizarGemeloDigital()
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
                anatomicalNode?.modelInstance?.materialInstances?.forEach { material ->
                    try {
                        material.setParameter("baseColorFactor", 0.8f, 0.35f, 0.3f, alpha)
                    } catch (e: Exception) {}
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun aplicarCapas(progress: Int) {
        anatomicalNode?.modelInstance?.materialInstances?.forEach { material ->
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
            } catch (e: Exception) {}
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

        heatmapNode?.let { node ->
            val materials = node.modelInstance?.materialInstances ?: return

            val factorHabitos = ((1.5f - litrosAgua).coerceAtLeast(0f) / 1.5f) + (if (sodioAlto) 0.3f else 0.0f)
            val severidadTotal = (nivelDanoIA + factorHabitos).coerceIn(0.0f, 1.0f)

            val deformacionX = 1.0f - (severidadTotal * 0.20f)
            val deformacionY = 1.0f - (severidadTotal * 0.15f)
            val deformacionZ = 1.0f - (severidadTotal * 0.10f)
            node.scale = Scale(deformacionX, deformacionY, deformacionZ)

            materials.forEach { material ->
                try {
                    val t = severidadTotal
                    val (r, g, b) = when {
                        t < 0.5f -> {
                            val localT = t / 0.5f
                            Triple(localT, 1.0f, 0.0f)
                        }
                        else -> {
                            val localT = (t - 0.5f) / 0.5f
                            Triple(1.0f, 1.0f - localT, 0.0f)
                        }
                    }
                    material.setParameter("baseColorFactor", r, g, b, 1.0f)
                    material.setParameter("emissiveFactor", r * 0.4f, g * 0.4f, b * 0.4f)
                } catch (e: Exception) {}
            }

            if (severidadTotal > 0.2f && !isPulseActive) {
                iniciarPulsoSimple(severidadTotal)
            } else if (severidadTotal <= 0.2f) {
                detenerPulsoSimple()
            }
        }
    }

    private fun iniciarPulsoSimple(severidad: Float) {
        detenerPulsoSimple()
        isPulseActive = true
        val runnable = object : Runnable {
            override fun run() {
                val factor = 0.5f + Math.sin((System.currentTimeMillis() / (200f / severidad)).toDouble()).toFloat() * 0.5f
                heatmapNode?.modelInstance?.materialInstances?.forEach { material ->
                    try {
                        material.setParameter("emissiveFactor", 0.5f * factor, 0.5f * factor, 0.5f * factor)
                    } catch (e: Exception) {}
                }
                pulseHandler.postDelayed(this, (100 / severidad).toLong())
            }
        }
        pulseRunnable = runnable
        pulseHandler.post(runnable)
    }

    private fun detenerPulsoSimple() {
        pulseRunnable?.let { pulseHandler.removeCallbacks(it) }
        pulseRunnable = null
        isPulseActive = false
        heatmapNode?.modelInstance?.materialInstances?.forEach { material ->
            try {
                material.setParameter("emissiveFactor", 0.5f, 0.5f, 0.5f)
            } catch (e: Exception) {}
        }
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
            anatomicalNode?.let { anatomicalSceneView.removeChild(it) }
            heatmapNode?.let { heatmapSceneView.removeChild(it) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        localClassifier.close()
    }
}