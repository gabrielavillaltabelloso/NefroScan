package com.insamt.nefroscan

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface DiagnosticDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarDiagnostico(diagnostico: DiagnosticEntity): Long

    // Esta es la función que te marca error:
    @Query("SELECT * FROM tabla_diagnosticos WHERE idMedicoAsignado = :idMedico OR idRegistrador = :idMedico ORDER BY fechaRegistroTimestamp DESC")
    suspend fun obtenerListaPorMedico(idMedico: String): List<DiagnosticEntity>

    @Query("SELECT * FROM tabla_diagnosticos WHERE idPaciente = :idPaciente ORDER BY fechaRegistroTimestamp DESC")
    suspend fun obtenerPorPaciente(idPaciente: String): List<DiagnosticEntity>

    @Query("SELECT * FROM tabla_diagnosticos WHERE idRegistrador = :idRegistrador ORDER BY fechaRegistroTimestamp DESC")
    suspend fun obtenerPorRegistrador(idRegistrador: String): List<DiagnosticEntity>

    @Query("SELECT * FROM tabla_diagnosticos ORDER BY fechaRegistroTimestamp DESC")
    suspend fun obtenerTodosLista(): List<DiagnosticEntity>

    @Query("SELECT * FROM tabla_diagnosticos WHERE sincronizadoConNube = 0")
    suspend fun obtenerPendientesDeSincronizar(): List<DiagnosticEntity>

    @Query("UPDATE tabla_diagnosticos SET sincronizadoConNube = 1 WHERE id = :id")
    suspend fun marcarComoSincronizado(id: Long)
}