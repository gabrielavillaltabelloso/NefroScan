package com.insamt.nefroscan

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface UserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarUsuario(usuario: UserEntity)

    @Query("SELECT * FROM tabla_usuarios WHERE idUsuario = :idUsuario AND contrasena = :contrasena LIMIT 1")
    suspend fun autenticar(idUsuario: String, contrasena: String): UserEntity?

    @Query("SELECT * FROM tabla_usuarios WHERE idUsuario = :idUsuario LIMIT 1")
    suspend fun obtenerUsuarioPorId(idUsuario: String): UserEntity?

    @Query("SELECT COUNT(*) FROM tabla_usuarios")
    suspend fun contarUsuarios(): Int
}