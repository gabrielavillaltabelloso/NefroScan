package com.insamt.nefroscan

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.abs

object ImageQualityEvaluator {

    data class QualityResult(
        val isApta: Boolean,
        val nitidezScore: Double,
        val brilloPromedio: Double,
        val mensajeDiagnostico: String
    )

    fun evaluarCalidadEcografia(bitmap: Bitmap): QualityResult {
        // Redimensionar para análisis rápido sin bloquear la UI
        val scaled = Bitmap.createScaledBitmap(bitmap, 200, 200, true)
        val width = scaled.width
        val height = scaled.height

        val pixels = IntArray(width * height)
        scaled.getPixels(pixels, 0, width, 0, 0, width, height)

        val gray = DoubleArray(width * height)
        var sumaBrillo = 0.0

        for (i in pixels.indices) {
            val p = pixels[i]
            val r = Color.red(p)
            val g = Color.green(p)
            val b = Color.blue(p)
            val luminancia = 0.299 * r + 0.587 * g + 0.114 * b
            gray[i] = luminancia
            sumaBrillo += luminancia
        }

        val brilloPromedio = sumaBrillo / (width * height)

        // Cálculo de nitidez mediante varianza del operador Laplaciano (kernel 3x3)
        var sumaLaplaciano = 0.0
        var sumaCuadradosLaplaciano = 0.0
        var count = 0

        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val center = gray[y * width + x]
                val top = gray[(y - 1) * width + x]
                val bottom = gray[(y + 1) * width + x]
                val left = gray[y * width + (x - 1)]
                val right = gray[y * width + (x + 1)]

                // Convolución Laplaciana: 4*Centro - (Arriba + Abajo + Izquierda + Derecha)
                val lap = abs(4 * center - (top + bottom + left + right))
                sumaLaplaciano += lap
                sumaCuadradosLaplaciano += lap * lap
                count++
            }
        }

        val mediaLap = sumaLaplaciano / count
        val varianzaLaplaciana = (sumaCuadradosLaplaciano / count) - (mediaLap * mediaLap)

        // Criterios clínicos de rechazo
        return when {
            brilloPromedio < 30.0 -> {
                QualityResult(
                    isApta = false,
                    nitidezScore = varianzaLaplaciana,
                    brilloPromedio = brilloPromedio,
                    mensajeDiagnostico = "Imagen subexpuesta (demasiado oscura). Aumente la ganancia acústica del ecógrafo."
                )
            }
            brilloPromedio > 220.0 -> {
                QualityResult(
                    isApta = false,
                    nitidezScore = varianzaLaplaciana,
                    brilloPromedio = brilloPromedio,
                    mensajeDiagnostico = "Saturación de brillo / artefacto de reverberación. Ajuste la profundidad de foco."
                )
            }
            varianzaLaplaciana < 85.0 -> {
                QualityResult(
                    isApta = false,
                    nitidezScore = varianzaLaplaciana,
                    brilloPromedio = brilloPromedio,
                    mensajeDiagnostico = "Imagen borrosa / fuera de foco. Estabilice el transductor y repita la captura."
                )
            }
            else -> {
                QualityResult(
                    isApta = true,
                    nitidezScore = varianzaLaplaciana,
                    brilloPromedio = brilloPromedio,
                    mensajeDiagnostico = "Calidad óptima para análisis morfológico e inferencia IA."
                )
            }
        }
    }
}