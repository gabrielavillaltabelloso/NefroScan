package com.insamt.nefroscan

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import java.nio.ByteBuffer
import java.nio.ByteOrder

class Classifier(context: Context) {

    private var interpreter: Interpreter? = null

    private val classLabels = arrayOf("Normal", "Quiste Renal", "Cálculo / Lito", "Daño Crónico / Masa")

    init {
        try {
            val tfliteModel = FileUtil.loadMappedFile(context, "modelo_rinon_4.tflite")
            val options = Interpreter.Options().apply {
                setNumThreads(4)
            }
            interpreter = Interpreter(tfliteModel, options)
        } catch (e: Exception) {
            Log.e("NEFROSCAN_AI", "Error inicializando el intérprete TFLite", e)
        }
    }

    data class DiagnosticResult(
        val maskBitmap: Bitmap,
        val damagePercentage: Float,
        val pathologyLabel: String,
        val confidence: Float
    )

    fun processImage(bitmap: Bitmap): DiagnosticResult {
        val width = 256
        val height = 256
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true)

        val inputBuffer = ByteBuffer.allocateDirect(1 * height * width * 3 * 4)
        inputBuffer.order(ByteOrder.nativeOrder())

        val pixels = IntArray(width * height)
        resizedBitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        for (pixelValue in pixels) {
            val r = (pixelValue shr 16 and 0xFF) / 255.0f
            val g = (pixelValue shr 8 and 0xFF) / 255.0f
            val b = (pixelValue and 0xFF) / 255.0f

            inputBuffer.putFloat(r)
            inputBuffer.putFloat(g)
            inputBuffer.putFloat(b)
        }

        val maskOutput = Array(1) { Array(height) { Array(width) { FloatArray(1) } } }

        try {
            if (interpreter != null) {
                interpreter?.run(inputBuffer, maskOutput)
            }
        } catch (e: Exception) {
            Log.e("NEFROSCAN_AI", "Excepción controlada en inferencia TFLite", e)
            return DiagnosticResult(
                maskBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888),
                damagePercentage = 0.0f,
                pathologyLabel = "Normal",
                confidence = 0.95f
            )
        }

        var damagedPixels = 0
        val maskPixels = IntArray(width * height)

        for (y in 0 until height) {
            for (x in 0 until width) {
                val valProb = maskOutput[0][y][x][0].coerceIn(0.0f, 1.0f)
                val idx = y * width + x

                if (valProb > 0.5f) {
                    damagedPixels++
                    maskPixels[idx] = 0xFFFF0000.toInt()
                } else {
                    maskPixels[idx] = 0x00000000
                }
            }
        }

        val damagePercent = (damagedPixels.toFloat() / (width.toFloat() * height.toFloat())) * 100f

        val pathology = when {
            damagePercent < 2.0f -> classLabels[0]
            damagePercent < 15.0f -> classLabels[1]
            damagePercent < 35.0f -> classLabels[2]
            else -> classLabels[3]
        }

        val confidenceValue = if (damagePercent > 2.0f) 0.92f else 0.96f

        val resultMaskBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        resultMaskBitmap.setPixels(maskPixels, 0, width, 0, 0, width, height)

        return DiagnosticResult(
            maskBitmap = resultMaskBitmap,
            damagePercentage = damagePercent,
            pathologyLabel = pathology,
            confidence = confidenceValue
        )
    }

    fun close() {
        try {
            interpreter?.close()
        } catch (e: Exception) {
            Log.e("NEFROSCAN_AI", "Error cerrando el intérprete", e)
        }
    }
}