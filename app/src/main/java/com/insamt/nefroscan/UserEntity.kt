package com.insamt.nefroscan

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tabla_usuarios")
data class UserEntity(
    @PrimaryKey val idUsuario: String, // DUI o Correo
    val nombreCompleto: String,
    val contrasena: String,
    val rol: String, // "MEDICO", "PROMOTOR", "PACIENTE"
    val detalleAdicional: String? = null
)