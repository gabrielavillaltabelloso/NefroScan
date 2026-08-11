package com.insamt.nefroscan

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tabla_diagnosticos")
data class DiagnosticEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nombrePaciente: String,
    val edadPaciente: Int,
    val porcentajeDano: Float,
    val patologiaDetectada: String,
    val nivelSeveridad: String,
    val litrosAguaDiarios: Float,
    val nivelSodio: String,
    val egfrEstimado5Anios: Int,
    val egfrEstimado10Anios: Int,
    val fechaRegistroTimestamp: Long = System.currentTimeMillis(),
    val sincronizadoConNube: Boolean = false
)