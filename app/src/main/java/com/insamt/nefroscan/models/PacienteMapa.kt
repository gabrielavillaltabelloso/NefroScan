package com.insamt.nefroscan.models

data class PacienteMapa(
    val uid: String = "",              // UID de Firebase Auth
    val correo: String = "",           // Correo con el que se registró
    val nombre: String = "",           // Nombre completo del paciente
    val direccion: String = "",        // Descripción o cantón
    val latitud: Double = 0.0,         // Coordenada latitud
    val longitud: Double = 0.0,        // Coordenada longitud
    val nivelRiesgo: String = "BAJO",  // "ALTO", "MEDIO", "BAJO"
    val factoresExposicion: List<String> = emptyList(),
    val telefono: String = "",
    val rol: String = "paciente"
)