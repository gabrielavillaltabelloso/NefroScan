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

    // 1. PACIENTE: Solo sus propios diagnósticos
    @Query("SELECT * FROM tabla_diagnosticos WHERE idPaciente = :idPaciente ORDER BY fechaRegistroTimestamp DESC")
    fun obtenerDiagnosticosPorPaciente(idPaciente: String): Flow<List<DiagnosticEntity>>

    @Query("SELECT * FROM tabla_diagnosticos WHERE idPaciente = :idPaciente ORDER BY fechaRegistroTimestamp DESC")
    suspend fun obtenerListaPorPaciente(idPaciente: String): List<DiagnosticEntity>

    // 2. PROMOTOR: Registros que él mismo levantó en campo
    @Query("SELECT * FROM tabla_diagnosticos WHERE idRegistrador = :idPromotor ORDER BY fechaRegistroTimestamp DESC")
    fun obtenerDiagnosticosPorPromotor(idPromotor: String): Flow<List<DiagnosticEntity>>

    @Query("SELECT * FROM tabla_diagnosticos WHERE idRegistrador = :idPromotor ORDER BY fechaRegistroTimestamp DESC")
    suspend fun obtenerListaPorPromotor(idPromotor: String): List<DiagnosticEntity>

    // 3. MÉDICO: Pacientes asignados a él O diagnósticos registrados por él
    @Query("""
        SELECT * FROM tabla_diagnosticos 
        WHERE idMedicoAsignado = :idMedico OR idRegistrador = :idMedico 
        ORDER BY fechaRegistroTimestamp DESC
    """)
    fun obtenerDiagnosticosPorMedico(idMedico: String): Flow<List<DiagnosticEntity>>

    @Query("""
        SELECT * FROM tabla_diagnosticos 
        WHERE idMedicoAsignado = :idMedico OR idRegistrador = :idMedico 
        ORDER BY fechaRegistroTimestamp DESC
    """)
    suspend fun obtenerListaPorMedico(idMedico: String): List<DiagnosticEntity>

    // Utilidades
    @Query("SELECT * FROM tabla_diagnosticos WHERE id = :id LIMIT 1")
    suspend fun obtenerDiagnosticoPorId(id: Long): DiagnosticEntity?

    @Query("SELECT * FROM tabla_diagnosticos WHERE sincronizadoConNube = 0")
    suspend fun obtenerPendientesDeSincronizar(): List<DiagnosticEntity>

    @Query("UPDATE tabla_diagnosticos SET sincronizadoConNube = 1 WHERE id = :id")
    suspend fun marcarComoSincronizado(id: Long)

    // 🚀 AGREGADA: Permite a HistorialActivity filtrar los expedientes por paciente
    @Query("SELECT * FROM tabla_diagnosticos WHERE nombrePaciente = :nombre ORDER BY fechaRegistroTimestamp DESC")
    suspend fun obtenerDiagnosticosPorPaciente(nombre: String): List<DiagnosticEntity>
}