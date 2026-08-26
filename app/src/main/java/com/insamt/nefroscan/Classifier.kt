package com.insamt.nefroscan

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import java.nio.ByteBuffer
import java.nio.ByteOrder

class Classifier(private val context: Context) {

    data class DiagnosticResult(
        val pathologyLabel: String,
        val confidence: Float,
        val normalPercentage: Float,
        val anomalyPercentage: Float,
        val nephropathyPercentage: Float
    )

    private var interpreter: Interpreter? = null
    private val inputImageWidth = 224
    private val inputImageHeight = 224
    private val channels = 3

    init {
        try {
            val modelBuffer = FileUtil.loadMappedFile(context, "nefroscan_3clases.tflite")
            val options = Interpreter.Options().apply {
                setNumThreads(4)
                setUseXNNPACK(true)
            }
            interpreter = Interpreter(modelBuffer, options)
            Log.d("NefroScanIA", "Modelo Multiclase TFLite cargado correctamente.")
        } catch (e: Exception) {
            Log.e("NefroScanIA", "Error al inicializar TFLite: ${e.message}")
            interpreter = null
        }
    }

    fun processImage(bitmap: Bitmap): DiagnosticResult? {
        val currentInterpreter = interpreter ?: run {
            Log.e("NefroScanIA", "Intérprete nulo. El modelo no cargó.")
            return null
        }

        return try {
            val resizedBitmap = Bitmap.createScaledBitmap(bitmap, inputImageWidth, inputImageHeight, true)

            val inputBuffer = ByteBuffer.allocateDirect(4 * inputImageWidth * inputImageHeight * channels).apply {
                order(ByteOrder.nativeOrder())
                rewind()
            }

            val intValues = IntArray(inputImageWidth * inputImageHeight)
            resizedBitmap.getPixels(intValues, 0, inputImageWidth, 0, 0, inputImageWidth, inputImageHeight)

            for (pixelValue in intValues) {
                val r = ((pixelValue shr 16) and 0xFF).toFloat()
                val g = ((pixelValue shr 8) and 0xFF).toFloat()
                val b = (pixelValue and 0xFF).toFloat()

                inputBuffer.putFloat(r)
                inputBuffer.putFloat(g)
                inputBuffer.putFloat(b)
            }

            inputBuffer.rewind()

            val outputBuffer = Array(1) { FloatArray(3) }
            currentInterpreter.run(inputBuffer, outputBuffer)

            val probabilities = outputBuffer[0]
            val normalProb = probabilities[0]
            val anomalyProb = probabilities[1]
            val nephropathyProb = probabilities[2]

            var maxProb = normalProb
            var label = "Riñón Normal"

            if (anomalyProb > maxProb) {
                maxProb = anomalyProb
                label = "Anomalía Física (Litiasis / Quiste)"
            }
            if (nephropathyProb > maxProb) {
                maxProb = nephropathyProb
                label = "Posible Nefropatía / Falla Renal"
            }

            DiagnosticResult(
                pathologyLabel = label,
                confidence = maxProb * 100f,
                normalPercentage = normalProb * 100f,
                anomalyPercentage = anomalyProb * 100f,
                nephropathyPercentage = nephropathyProb * 100f
            )

        } catch (e: Exception) {
            Log.e("NefroScanIA", "Error en inferencia de tensores: ${e.message}")
            null
        }
    }

    fun close() {
        interpreter?.close()
        interpreter = null
    }
}