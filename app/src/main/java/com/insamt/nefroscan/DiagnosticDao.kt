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

    // Esta función la usas si necesitas un Flow en vivo
    @Query("SELECT * FROM tabla_diagnosticos ORDER BY fechaRegistroTimestamp DESC")
    fun obtenerTodosLosDiagnosticos(): Flow<List<DiagnosticEntity>>

    // NUEVA: Esta es la que resuelve el error en PacienteDashboardActivity
    @Query("SELECT * FROM tabla_diagnosticos ORDER BY fechaRegistroTimestamp ASC")
    suspend fun obtenerTodos(): List<DiagnosticEntity>

    @Query("SELECT * FROM tabla_diagnosticos WHERE id = :id LIMIT 1")
    suspend fun obtenerDiagnosticoPorId(id: Long): DiagnosticEntity?

    @Query("SELECT * FROM tabla_diagnosticos WHERE sincronizadoConNube = 0")
    suspend fun obtenerPendientesDeSincronizar(): List<DiagnosticEntity>

    @Query("UPDATE tabla_diagnosticos SET sincronizadoConNube = 1 WHERE id = :id")
    suspend fun marcarComoSincronizado(id: Long)
}