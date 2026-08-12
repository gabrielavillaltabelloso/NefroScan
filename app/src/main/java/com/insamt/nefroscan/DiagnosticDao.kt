package com.insamt.nefroscan

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DiagnosticDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarDiagnostico(diagnostico: DiagnosticEntity): Long

    @Query("SELECT * FROM tabla_diagnosticos ORDER BY fechaRegistroTimestamp DESC")
    fun obtenerTodosLosDiagnosticos(): Flow<List<DiagnosticEntity>>

    // Consulta suspend directa que utiliza el MedicoDashboardActivity
    @Query("SELECT * FROM tabla_diagnosticos ORDER BY fechaRegistroTimestamp DESC")
    suspend fun obtenerTodosLista(): List<DiagnosticEntity>

    @Query("SELECT * FROM tabla_diagnosticos WHERE id = :id LIMIT 1")
    suspend fun obtenerDiagnosticoPorId(id: Long): DiagnosticEntity?

    @Query("SELECT * FROM tabla_diagnosticos WHERE sincronizadoConNube = 0")
    suspend fun obtenerPendientesDeSincronizar(): List<DiagnosticEntity>

    @Query("UPDATE tabla_diagnosticos SET sincronizadoConNube = 1 WHERE id = :id")
    suspend fun marcarComoSincronizado(id: Long)
}