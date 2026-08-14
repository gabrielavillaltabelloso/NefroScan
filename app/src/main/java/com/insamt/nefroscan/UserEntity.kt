package com.insamt.nefroscan

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tabla_usuarios")
data class UserEntity(
    @PrimaryKey val idUsuario: String, // DUI o Correo (Ej: "01234567-8")
    val nombreCompleto: String,
    val contrasena: String,
    val rol: String, // "MEDICO", "PROMOTOR", "PACIENTE"
    val detalleAdicional: String = "" // Ej: "Especialista Nefrología" o "Caserío El Espino"
)