package com.insamt.nefroscan.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "evaluaciones_edema")
data class EdemaEvaluacion(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val pacienteId: String = "",
    val fecha: Long = System.currentTimeMillis(),

    // Parámetros Físicos
    val foveaGrado: Int, // 0 a 4
    val foveaDescripcion: String,
    val ubicacion: String,
    val esBilateral: Boolean, // Clave: bilateral = renal / unilateral = vascular
    val aumentoPesoKg: Double,

    // Parámetros Clínicos Complementarios
    val disminucionDiuresis: Boolean,
    val tieneDisnea: Boolean,
    val tieneOrtopnea: Boolean,

    // Multimedia y TFLite
    val videoUriLocal: String?,
    var videoCloudUrl: String? = null,

    // Resultados del Triage
    val scoreSobrecarga: Int,
    val nivelRiesgo: String, // "NORMAL", "LEVE_MODERADO", "ALERTA_ROJA"
    val alertaCardiopulmonar: Boolean,
    val sospechaTvpUnilateral: Boolean,
    val enviadoAlMedico: Boolean = false
)