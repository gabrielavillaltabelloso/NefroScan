package com.insamt.nefroscan

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tabla_diagnosticos")
data class DiagnosticEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,

    // Trazabilidad de roles y pertenencia
    val idPaciente: String,               // DUI/Correo del Paciente
    val idRegistrador: String,            // DUI/Correo de quien creó el registro (Médico o Promotor)
    val rolRegistrador: String,           // "MEDICO" o "PROMOTOR"
    val idMedicoAsignado: String? = null, // Médico a cargo del paciente

    // Datos clínicos
    val nombrePaciente: String,
    val edadPaciente: Int,
    val porcentajeDano: Double,
    val patologiaDetectada: String,
    val nivelSeveridad: String,
    val litrosAguaDiarios: Double,
    val nivelSodio: Double,
    val egfrEstimado5Anios: Double,
    val egfrEstimado10Anios: Double,
    val fechaRegistroTimestamp: Long = System.currentTimeMillis(),
    val sincronizadoConNube: Boolean = false
)