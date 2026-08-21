package com.insamt.nefroscan

import kotlin.math.exp
import kotlin.math.pow

object CoxSurvivalEngine {

    data class PuntoSobrevida(
        val anio: Int,
        val probabilidadBase: Double,       // % de no requerir diálisis sin intervención
        val probabilidadIntervencion: Double // % de no requerir diálisis con intervención médica
    )

    data class SimulacionResult(
        val puntos: List<PuntoSobrevida>,
        val gananciaAniosLibres: Double,
        val reduccionRiesgoRelativo: Double
    )

    /**
     * @param egfr Tasa de filtrado glomerular actual (mL/min/1.73m²)
     * @param edad Edad del paciente
     * @param danoPorcentaje Porcentaje de daño detectado por IA (0.0 a 100.0)
     * @param usoAines Si el paciente consume antiinflamatorios nefrotóxicos regularmente
     * @param hipertensionDescontrolada Si mantiene PA > 140/90 mmHg
     */
    fun simularSobrevida(
        egfr: Double,
        edad: Double,
        danoPorcentaje: Double,
        usoAines: Boolean,
        hipertensionDescontrolada: Boolean
    ): SimulacionResult {

        // Coeficientes beta calibrados según modelo de riesgos proporcionales de Cox
        val betaEgfr = -0.045
        val betaDano = 0.035
        val betaEdad = 0.015
        val betaAines = 0.65
        val betaHta = 0.45

        // Score log-hazard del escenario basal
        val logHazardBase = (betaEgfr * (egfr - 60.0)) +
                (betaDano * (danoPorcentaje - 20.0)) +
                (betaEdad * (edad - 50.0)) +
                (if (usoAines) betaAines else 0.0) +
                (if (hipertensionDescontrolada) betaHta else 0.0)

        val hazardRatioBase = exp(logHazardBase)

        // Escenario con Intervención Nefroprotectora NefroScan:
        // Cero AINEs, Presión controlada y desaceleración del daño en 30%
        val logHazardOptimo = (betaEgfr * (egfr - 60.0)) +
                (betaDano * ((danoPorcentaje * 0.7) - 20.0)) +
                (betaEdad * (edad - 50.0))
        val hazardRatioOptimo = exp(logHazardOptimo)

        // Función de sobrevida basal S0(t) estandarizada por año (1 a 10)
        val s0PorAnio = doubleArrayOf(0.995, 0.985, 0.968, 0.945, 0.915, 0.880, 0.840, 0.790, 0.730, 0.660)

        val puntos = mutableListOf<PuntoSobrevida>()
        var sumaProbBase = 0.0
        var sumaProbOptima = 0.0

        for (i in s0PorAnio.indices) {
            val anio = i + 1
            val s0 = s0PorAnio[i]

            // S(t) = S0(t)^exp(beta * X)
            val probBase = (s0.pow(hazardRatioBase) * 100.0).coerceIn(1.0, 99.9)
            val probOptima = (s0.pow(hazardRatioOptimo) * 100.0).coerceIn(1.0, 99.9)

            puntos.add(PuntoSobrevida(anio, probBase, probOptima))
            sumaProbBase += (probBase / 100.0)
            sumaProbOptima += (probOptima / 100.0)
        }

        val gananciaAnios = sumaProbOptima - sumaProbBase
        val rrr = ((hazardRatioBase - hazardRatioOptimo) / hazardRatioBase) * 100.0

        return SimulacionResult(
            puntos = puntos,
            gananciaAniosLibres = gananciaAnios.coerceAtLeast(0.0),
            reduccionRiesgoRelativo = rrr.coerceIn(0.0, 95.0)
        )
    }
}